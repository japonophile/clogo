(ns clogo.parser
  (:require [clojure.string :refer [trim split]]
            [clogo.util :refer [formatstr round]]
            [clogo.memory :refer :all]))

(def atomic-commands
  {
   :quitte       []
   :montretortue []
   :cachetortue  []
   :nettoie      []
   :avance       [ :number ]
   :recule       [ :number ]
   :gauche       [ :number ]
   :droite       [ :number ]
   :levecrayon   []
   :baissecrayon []
   :aupas        []
   :augalop      []
  })

(def keywords
  {
   "QUITTE" :quitte "QT" :quitte
   "MONTRETORTUE" :montretortue "MT" :montretortue
   "CACHETORTUE" :cachetortue "CT" :cachetortue
   "NETTOIE" :nettoie
   "AVANCE" :avance "AV" :avance
   "RECULE" :recule "RE" :recule
   "GAUCHE" :gauche "TG" :gauche
   "DROITE" :droite "TD" :droite
   "LEVECRAYON" :levecrayon "LC" :levecrayon
   "BAISSECRAYON" :baissecrayon "BC" :baissecrayon
   "REPETE" :repete
   "[" :ouvrecrochet "]" :fermecrochet
   "(" :ouvreparenthese ")" :fermeparenthese
   "DONNE" :donne
   "POUR" :pour "FIN" :fin
   "ECRIS" :ecris "MONTRE" :ecris
   "AUPAS" :aupas "AUTROT" :autrot "AUGALOP" :augalop 
  })

(def operators
  {
   "+" {:name "+" :fun + :precedence 1}
   "-" {:name "-" :fun - :precedence 1}
   "*" {:name "*" :fun * :precedence 2}
   "/" {:name "/" :fun / :precedence 2}
  })

(def builtin-functions
  {
   "SOMME"      {:name "SOMME"      :fun + :arg-syntax [:number :number]}
   "DIFFERENCE" {:name "DIFFERENCE" :fun - :arg-syntax [:number :number]}
   "PRODUIT"    {:name "PRODUIT"    :fun * :arg-syntax [:number :number]}
   "QUOTIENT"   {:name "QUOTIENT"   :fun / :arg-syntax [:number :number]}
  })

(declare parse-expression parse-multiple-cmd)

(defmacro check-error
  ([result-to-check mem else-block]
    `(check-error ~result-to-check ~mem "PAS ASSEZ D'ENTREE" ~else-block))
  ([result-to-check mem no-match-error else-block]
    `(check-error ~result-to-check ~mem ~no-match-error "{1}" ~else-block))
  ([result-to-check mem no-match-error error-fmt else-block]
    `(cond
       (= :incomplete ~result-to-check) [:incomplete ~mem]
       (= :no-match ~result-to-check) [[:error (formatstr ~error-fmt ~no-match-error)] ~mem]
       (= :error (first ~result-to-check)) [[:error (formatstr ~error-fmt (second ~result-to-check))] ~mem]
       :else ~else-block)))

(defn safely-apply
  "Applies a function to its arguments, or returns :unbound if any of its argument is :unbound"
  [f args]
  (if (every? #(not= :unbound %) args)
    (apply f args)
    :unbound))

(defn parse-number
  "Reads a number from a string. Returns nil if not a number."
  [s]
  (if (re-find #"^-?\d+\.?\d*$" s)
    (read-string s)))

(defn parse-nested-expression
  "Parses a nested expression, that is, an expression within parentheses."
  [words-to-parse mem]
  (if (= :ouvreparenthese (keywords (first words-to-parse)))
    (let [[parse-result newmem] (parse-expression (rest words-to-parse) mem)]
      (check-error parse-result newmem "EXPRESSION IMBRIQUEE INVALIDE"
                   (let [[value remaining-words] parse-result]
                     (if (= :fermeparenthese (keywords (first remaining-words)))
                       [[value (rest remaining-words)] newmem]
                       [[:error "PAS DE PARENTHESE FERMANTE"] newmem]))))
    [:no-match mem]))

(defn parse-operand
  "Parses an operand, which can be a number, a variable or a nested expression."
  [words-to-parse mem]
  (if (= [] words-to-parse)
    [:no-match mem]
    (if-let [n (parse-number (first words-to-parse))]
      [[n (rest words-to-parse)] mem]
      (if-let [n ((mem :vars) (first words-to-parse))]
        [[n (rest words-to-parse)] mem]
        (parse-nested-expression words-to-parse mem)))))

(defn with-nextop-do
  "If the next token is an operator, invoke the <todo-with-nextop> function with the computed <amount>,
   the new accumulated value <newacc>, the next operator <nextop>, <remaining-words> and <newmem>."
  [acc currentop parse-result newmem todo-with-nextop]
  (let [[amount remaining-words] parse-result
        newacc (safely-apply (currentop :fun) [acc amount])]
    (if-let [nextop (operators (first remaining-words))]
      (apply todo-with-nextop [amount newacc nextop (rest remaining-words) newmem])
      [[newacc remaining-words] newmem])))

(defn has-higher-precedence? [op1 op2] (>= (op1 :precedence) (op2 :precedence)))

(defn parse-infix-expression
  "Parses an infix expression.  This function is recursive and must be passed the accumulated value and the current operator."
  [acc currentop words-to-parse mem]
  (let [[parse-result newmem] (parse-operand words-to-parse mem)]
    (check-error parse-result newmem "EXPRESSION INCOMPLETE" (formatstr "{1} APRES {2}" "{1}" (currentop :name))
                 ;; OK, operand parsed successfully
                 (with-nextop-do acc currentop parse-result newmem
                   (fn [amount newacc nextop remaining-words newmem]
                     (if (has-higher-precedence? currentop nextop)
                       ;; continue parsing the expression
                       (parse-infix-expression newacc nextop remaining-words newmem)
                       ;; first parse the higher precedence subexpression,
                       ;; then accumulate the result and continue parsing the expression
                       (let [[parse-result newmem] (parse-infix-expression amount nextop remaining-words newmem)]
                         (if (= :error (first parse-result))
                           [parse-result newmem]
                           (with-nextop-do acc currentop parse-result newmem #(parse-infix-expression %2 %3 %4 %5))))))))))

(defn parse-arg
  "Parses a single argument (which can be composed of multiple words)."
  [arg-syntax words-to-parse mem]
		(case arg-syntax
		  :number (parse-expression words-to-parse mem)
      [[:error (formatstr "ERREUR INTERNE: FORMAT {1} INCONNU" arg-syntax)] mem]))

(defn parse-args
  "Parses multiple arguments from a collection of words.  Returns arguments and remaining words."
  ([arg-syntax words-to-parse mem] (parse-args arg-syntax words-to-parse mem []))
  ([arg-syntax words-to-parse mem args]
    (if (= [] arg-syntax)
      [[args words-to-parse] mem]
      (if (= [] words-to-parse)
        [[:error "PAS ASSEZ D'ENTREE"] mem]
        (let [[parse-result newmem] (parse-arg (first arg-syntax) words-to-parse mem)]
          (check-error parse-result newmem "MAUVAIS PARAMETRE"
                       (let [[arg-value remaining-words] parse-result]
                         (parse-args (rest arg-syntax) remaining-words newmem (conj args arg-value)))))))))

(defn parse-built-in-function-call
  "Parses a (built-in) function call"
  [words-to-parse mem]
  (let [function-name (first words-to-parse)]
    (if-let [builtin-fn (builtin-functions function-name)]
      (let [[parse-result newmem] (parse-args (builtin-fn :arg-syntax) (rest words-to-parse) mem)]
        (if (= :error (first parse-result))
          [[:error (formatstr "{1} POUR {2}" (second parse-result) function-name)] newmem]
          (let [[args remaining-words] parse-result]
            [[(safely-apply (builtin-fn :fun) args) remaining-words] newmem])))
      [:no-match mem])))

(defn parse-expression
  "Parses a numerical expression and return its value."
  [words-to-parse mem]
  (let [[parse-result newmem] (parse-built-in-function-call words-to-parse mem)]
    (cond
      (= :no-match parse-result)
      (let [[parse-result newmem] (parse-operand words-to-parse mem)]
        (if (or (= :no-match parse-result) (= :error (first parse-result)))
          [parse-result newmem]
          ;; we have a valid operand, check whether this could be an infix expression
          (let [[value remaining-words] parse-result]
            (if-let [oper (operators (first remaining-words))]
              (parse-infix-expression value oper (rest remaining-words) newmem)
              [parse-result newmem]))))
      (= :error (first parse-result)) [parse-result newmem]
      :else [parse-result newmem])))

(defn parse-atomic-cmd
  "Parses an atomic command, that is, one that does not have nested commands."
  [words-to-parse mem]
    (let [action-name (first words-to-parse)
          action (keywords action-name)]
      (if-let [arg-syntax (atomic-commands action)]
        (let [[parse-result newmem] (parse-args arg-syntax (rest words-to-parse) mem)]
          (if (= :error (first parse-result))
            [[:error (formatstr "{1} POUR {2}" (second parse-result) action-name)] newmem]
            [[action (first parse-result) (second parse-result)] newmem]))
        [:no-match mem])))

(defn parse-cmd-block
  "Parses a block of commands, consisting of multiple commands within brackets."
  [words-to-parse mem]
	  (if (= :ouvrecrochet (keywords (first words-to-parse)))
	    (let [[parse-result _] (parse-multiple-cmd (rest words-to-parse) mem)]
	      (check-error parse-result mem
                    (let [nested-commands (butlast parse-result)
                          remaining-words (last parse-result)]
                      (if (= :fermecrochet (keywords (first remaining-words)))
                        [(conj (vec nested-commands) (rest remaining-words)) mem]
                        (if (= [] remaining-words)
                          [:incomplete mem]
                          [[:error "PAS DE CROCHET FERMANT"] mem])))))
	    [:no-match mem]))

(defn parse-repeat-cmd
  "Parses a repeat command, with nested command block."
  [words-to-parse mem]
    (let [action (keywords (first words-to-parse))]
      (if (= :repete action)
        (let [[parse-result newmem] (parse-arg :number (rest words-to-parse) mem)]
          (check-error parse-result newmem "MAUVAIS PARAMETRE" (formatstr "{1} POUR {2}" "{1}" "REPETE")
                       (let [[n remaining-words] parse-result
                             times-to-repeat (if (= :unbound n) 1 n)
                             [repeat-body newmem2] (parse-cmd-block remaining-words newmem)]
                         (check-error repeat-body newmem2 "PAS DE CROCHET OUVRANT"
                                      (let [commands-to-repeat (butlast repeat-body)
                                            remaining-words (last repeat-body)]
                                        [(conj (vec (apply concat (repeat times-to-repeat commands-to-repeat))) remaining-words) newmem2])))))
        [:no-match mem])))

(defn valid-name?
  "Determines whether the supplied word is a valid function or variable name"
  [word]
  (and (not (nil? word))
       (nil? (keywords word))
       (nil? (builtin-functions word))
       (= 1 (count (re-seq #"^[A-z][A-z0-9]*$" word)))))

(defn parse-assign-cmd
  "Parses a variable assignment."
  [words-to-parse mem]
  (let [action (keywords (first words-to-parse))
        variable-name (second words-to-parse)]
    (if (= :donne action)
      (if-not (valid-name? variable-name)
        [[:error (formatstr "NE PEUX APPELER UNE VARIABLE {1}" variable-name)] mem]
        (let [[parse-result newmem] (parse-arg :number (rest (rest words-to-parse)) mem)]
          (if (or (= :no-match parse-result) (= :error (first parse-result)))
            [[:error (formatstr "{1} POUR {2}" "MAUVAIS PARAMETRE" "DONNE")] newmem]
            (let [[value remaining-words] parse-result]
              [[remaining-words] (defvar mem variable-name value)]))))
      [:no-match mem])))

(defn parse-param
  "Parses a single parameter name"
  [words-to-parse]
  (if-let [param-name (first words-to-parse)]
    (let [param (re-seq #"^:[A-z][A-z0-9]*$" param-name)]
      (if (= 1 (count param))
        [(first param) (rest words-to-parse)]
        :no-match))
    :no-match))

(defn parse-params
  "Parses multiple parameter names"
  ([words-to-parse] (parse-params words-to-parse []))
  ([words-to-parse params]
    (let [parse-result (parse-param words-to-parse)]
      (if (= :no-match parse-result)
        [params words-to-parse]
        (let [[param-name remaining-words] parse-result]
          (parse-params remaining-words (conj params param-name)))))))

(defn parse-function-declaration
  "Parses a function declaration"
  [words-to-parse mem]
    (if (= :pour (keywords (first words-to-parse)))
      (if-let [function-name (second words-to-parse)]
        (if-not (valid-name? function-name)
          [[:error (formatstr "NE PEUX APPELER UNE FONCTION {1}" function-name)] mem]
          (let [[params remaining-words-before-body] (parse-params (rest (rest words-to-parse)))
                localmem (reduce #(defvar %1 %2 :unbound) mem params)
                with-brackets (= :ouvrecrochet (keywords (first remaining-words-before-body)))
                parse-function-body (if with-brackets parse-cmd-block parse-multiple-cmd)
                [parse-result _] (parse-function-body remaining-words-before-body localmem)]
            (check-error parse-result mem "PAS ASSEZ D'ENTREE" (formatstr "{1} POUR {2}" "{1}" function-name)
                         (let [remaining-words (last parse-result)]
                           (if (or with-brackets
                                   (= :fin (keywords (first remaining-words))))
                             ;; register new function in memory
                             (let [nwords (- (count remaining-words-before-body) (count remaining-words))
                                   fun-body-nwords (if with-brackets (- nwords 2) nwords)
                                   fun-body-words (if with-brackets (rest remaining-words-before-body) remaining-words-before-body)
                                   function-body (take fun-body-nwords fun-body-words)
                                   newmem (deffun mem function-name [params function-body])] 
                               [[(vec (if with-brackets remaining-words (rest remaining-words)))] newmem])
                             (if (= [] remaining-words)
                               [:incomplete mem]
                               [[:error (formatstr "PAS DE FIN A LA FONCTION {1}" function-name)] mem]))))))
        [[:error (formatstr "{1} POUR {2}" "PAS ASSEZ D'ENTREE" "POUR")] mem])
      [:no-match mem]))

(defn parse-function-invocation
  "Parses a command invocation"
  [words-to-parse mem]
  (let [function-name (first words-to-parse)]
    (if-let [function-def ((mem :funs) function-name)]
      (let [[arg-names function-body] function-def
            arg-syntax (vec (repeat (count arg-names) :number))
            [parse-result newmem] (parse-args arg-syntax (rest words-to-parse) mem)]
        (check-error parse-result newmem "PAS ASSEZ D'ENTREE" (formatstr "{1} POUR {2}" "{1}" function-name)
                     (let [[arg-values remaining-words] parse-result
                           localmem (assoc mem :vars (merge (mem :vars) (zipmap arg-names arg-values)))
                           [parse-result _] (parse-multiple-cmd function-body localmem)]
                       (check-error parse-result newmem (formatstr "NE SAIT COMMENT {1}" function-name)
                                    [(conj (vec (butlast parse-result)) remaining-words) newmem]))))
      [:no-match mem])))

(defn parse-show-cmd
  "Parses a show command"
  [words-to-parse mem]
  (if (= :ecris (keywords (first words-to-parse)))
    (if (second words-to-parse)
      (if-let [value ((:vars mem) (second words-to-parse))]
        [[:ecris [(str (round value 1))] (vec (rest (rest words-to-parse)))] mem]
        [[:error (formatstr "NE SAIS COMMENT FAIRE {1}" (second words-to-parse))] mem])
      [[:error (formatstr "{1} POUR {2}" "PAS ASSEZ D'ENTREE" (first words-to-parse))] mem])
    [:no-match mem]))

(defn parse-single-cmd
  "Parses a single command, which can be either an atomic command or a command with nested commands such as repeat."
  [words-to-parse mem]
  (do
    ;;(println words-to-parse)
    (let [[parse-result newmem] (parse-atomic-cmd words-to-parse mem)]
      (if (= :no-match parse-result)
        (let [[parse-result newmem] (parse-repeat-cmd words-to-parse mem)]
          (if (= :no-match parse-result)
            (let [[parse-result newmem] (parse-assign-cmd words-to-parse mem)]
              (if (= :no-match parse-result)
                (let [[parse-result newmem] (parse-function-declaration words-to-parse mem)]
                  (if (= :no-match parse-result)
                    (let [[parse-result newmem] (parse-function-invocation words-to-parse mem)]
                      (if (= :no-match parse-result)
                        (parse-show-cmd words-to-parse mem)
                        [parse-result newmem]))
                    [parse-result newmem]))
                [parse-result newmem]))
            [parse-result newmem]))
        [parse-result newmem]))))

(defn parse-multiple-cmd
  "Parses multiple commands, that is a series of single commands."
  ([words-to-parse mem] (parse-multiple-cmd words-to-parse mem []))
  ([words-to-parse mem parse-result]
    (if (= [] words-to-parse)
      [(conj (vec parse-result) []) mem]
      (let [[single-cmd newmem] (parse-single-cmd words-to-parse mem)]
        (cond
          (= :no-match single-cmd) [(conj (vec parse-result) words-to-parse) newmem]
          (= :incomplete single-cmd) [:incomplete newmem]
          (= :error (first single-cmd)) [single-cmd newmem]
          :else (parse-multiple-cmd (last single-cmd) newmem (into parse-result (butlast single-cmd))))))))

(defn parse-input
  "Parses user input."
  [to-parse]
  (let [trimmed (trim to-parse)
        words-to-parse (if (empty? trimmed) [] (split trimmed #"\s+"))
        [parse-result newmem] (parse-multiple-cmd words-to-parse @mem)]
    (cond
      (= :incomplete parse-result) :incomplete
      (= :error (first parse-result)) parse-result
      :else
      (if-not (empty? (last parse-result))
        [:error (formatstr "NE SAIS QUE FAIRE DE {1}" (first (last parse-result)))]
        (do
          (update-mem! newmem)
          (butlast parse-result))))))
