(ns clogo.parser-test
  (:require [clojure.test :refer :all]
            [clogo.parser :refer :all]
            [clogo.memory :refer :all]))

(deftest safely-apply-should-apply-a-function-to-its-arguments
  (testing "safely-apply should apply a function to its arguments"
    (is (= 5 (safely-apply + [2 3])))))

(deftest safely-apply-should-return-unbound-if-one-of-its-args-is-unbound
  (testing "safely-apply should return :unbound if one of its arguments is unbound"
    (is (= :unbound (safely-apply + [2 :unbound 3])))))

(deftest parse-number-should-parse-an-integer
  (testing "parse-number should parse an integer"
    (is (= 394 (parse-number "394")))))

(deftest parse-number-should-parse-a-float
  (testing "parse-number should parse a float"
    (is (= 0.3789 (parse-number "0.3789")))))

(deftest parse-number-should-return-nil-for-nan
  (testing "parse-number should return nil for NaN"
    (is (= nil (parse-number "0.37.89")))))

(deftest parse-expression-should-parse-a-number
  (testing "parse-expression should parse a number"
    (is (= [[5 []] {:vars {} :funs {}}] (parse-expression ["5"] {:vars {} :funs {}})))))

(deftest parse-expression-should-parse-a-simple-expression
  (testing "parse-expression should parse a simple expression"
    (is (= [[5 []] {:vars {} :funs {}}] (parse-expression ["2" "+" "3"] {:vars {} :funs {}})))
    (is (= [[9 []] {:vars {} :funs {}}] (parse-expression ["2" "+" "3" "+" "4"] {:vars {} :funs {}})))
    (is (= [[:error "EXPRESSION INCOMPLETE APRES -"] {:vars {} :funs {}}] (parse-expression ["2" "*" "3" "-"] {:vars {} :funs {}})))))

(deftest parse-expression-should-respect-operator-precedence
  (testing "parse-expression should respect operator precendence"
    (is (= [[14 []] {:vars {} :funs {}}] (parse-expression ["2" "+" "3" "*" "4"] {:vars {} :funs {}})))
    (is (= [[10 []] {:vars {} :funs {}}] (parse-expression ["2" "*" "3" "+" "4"] {:vars {} :funs {}})))
    (is (= [[2 []] {:vars {} :funs {}}] (parse-expression ["6" "/" "3" "*" "2" "+" "5" "+" "2" "*" "3" "-" "2" "+" "1" "-" "3" "*" "2" "*" "2"] {:vars {} :funs {}})))))

(deftest parse-expression-should-parse-subexpressions
  (testing "parse-expression should parse subexpression"
    (is (= [[14 []] {:vars {} :funs {}}] (parse-expression ["2" "*" "(" "3" "+" "4" ")"] {:vars {} :funs {}})))
    (is (= [[:error "PAS DE PARENTHESE FERMANTE APRES *"] {:vars {} :funs {}}] (parse-expression ["2" "*" "(" "3" "+" "4" "5"] {:vars {} :funs {}})))
    (is (= [[:error "EXPRESSION IMBRIQUEE INVALIDE APRES *"] {:vars {} :funs {}}] (parse-expression ["2" "*" "(" ")"] {:vars {} :funs {}})))))

(deftest parse-expression-should-parse-expression-containing-variables
  (testing "parse-expression should parse expression containing variables"
    (is (= [[14 []] {:vars {"TERM" 3} :funs {}}] (parse-expression ["2" "*" "(" "TERM" "+" "4" ")"] {:vars {"TERM" 3} :funs {}})))))

(deftest parse-expression-should-parse-built-in-function-calls
  (testing "parse-expression should parse built-in function calls"
    (is (= [[11 []] {:vars {} :funs {}}] (parse-expression ["SOMME" "7" "4"] {:vars {} :funs {}})))
    (is (= [[10 []] {:vars {} :funs {}}] (parse-expression ["QUOTIENT" "PRODUIT" "SOMME" "7" "3" "DIFFERENCE" "8" "6" "2"] {:vars {} :funs {}})))
    (is (= [[:error "PAS ASSEZ D'ENTREE POUR SOMME"] {:vars {} :funs {}}] (parse-expression ["SOMME" "7"] {:vars {} :funs {}})))
    (is (= [[15 []] {:vars {} :funs {}}] (parse-expression ["SOMME" "7" "4" "*" "2"] {:vars {} :funs {}})))
    (is (= [[22 []] {:vars {} :funs {}}] (parse-expression ["(" "SOMME" "7" "4" ")" "*" "2"] {:vars {} :funs {}})))))

(deftest parse-expression-should-parse-expression-containing-unbound-variables
  (testing "parse-expression should parse expression containing unbound variables"
    (is (= [[:unbound []] {:vars {"TERM" :unbound} :funs {}}] (parse-expression ["2" "*" "(" "TERM" "+" "4" ")"] {:vars {"TERM" :unbound} :funs {}})))))

(deftest parse-arg-should-parse-a-numeric-argument
  (testing "parse-arg should parse a numeric argument"
    (is (= [[92 []] {:vars {} :funs {}}] (parse-arg :number ["92"] {:vars {} :funs {}})))))

(deftest parse-arg-should-return-remaining-words
  (testing "parse-arg should return remaining words"
    (is (= [[1212 ["remaining" "words"]] {:vars {} :funs {}}] (parse-arg :number ["1212" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-arg-should-return-an-error-when-argument-cannot-be-parsed
  (testing "parse-arg should return an error when argument cannot be parsed"
    (is (= [:no-match {:vars {} :funs {}}] (parse-arg :number ["unparsable" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-arg-should-return-variable-value-when-variable-name-found
  (testing "parse-arg should return variable value when variable name found"
    (is (= [[3 []] {:vars {"DISTANCE" 3} :funs {}}] (parse-arg :number ["DISTANCE"] {:vars {"DISTANCE" 3} :funs {}})))))

(deftest parse-arg-should-parse-an-expression
  (testing "parse-arg should parse an expression"
    (is (= [[3 []] {:vars {} :funs {}}] (parse-arg :number ["2" "+" "1"] {:vars {} :funs {}})))))

(deftest parse-args-should-parse-multiple-arguments
  (testing "parse-args should parse multiple arguments"
    (is (= [[[1 22 3 44] []] {:vars {} :funs {}}] (parse-args [:number :number :number :number] ["1" "22" "3" "44"] {:vars {} :funs {}})))))

(deftest parse-args-should-say-not-enough-info-when-no-word-left
  (testing "parse-args should say not enough info when no word left"
    (is (= [[:error "PAS ASSEZ D'ENTREE"] {:vars {} :funs {}}] (parse-args [:number :number] ["9899"] {:vars {} :funs {}})))))

(deftest parse-args-should-say-wrong-parameter-when-arg-cannot-be-parsed
  (testing "parse-args should say wrong parameter when arg cannot be parsed"
    (is (= [[:error "MAUVAIS PARAMETRE"] {:vars {} :funs {}}] (parse-args [:number :number :number] ["2" "wrong" "3"] {:vars {} :funs {}})))))

(deftest parse-args-should-return-remaining-words
  (testing "parse-args should return remaining words"
    (is (= [[[1 2] ["remaining" "words"]] {:vars {} :funs {}}] (parse-args [:number :number] ["1" "2" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-atomic-cmd-should-parse-an-atomic-command
  (testing "parse-atomic-cmd should parse an atomic command"
    (is (= [[:avance [10] []] {:vars {} :funs {}}] (parse-atomic-cmd ["AV" "10"] {:vars {} :funs {}})))))

(deftest parse-atomic-cmd-should-return-remaining-words
  (testing "parse-atomic-cmd should return remaining words"
    (is (= [[:gauche [90] ["remaining" "words"]] {:vars {} :funs {}}] (parse-atomic-cmd ["GAUCHE" "90" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-atomic-cmd-should-return-no-match-if-not-atomic-command
  (testing "parse-atomic-cmd return :no-match if not atomic command"
    (is (= [:no-match {:vars {} :funs {}}] (parse-atomic-cmd ["UnknownCommand" "90"] {:vars {} :funs {}})))))

(deftest parse-atomic-cmd-should-return-error-in-case-of-error
  (testing "parse-atomic-cmd return :error in case of error"
    (is (= [[:error "PAS ASSEZ D'ENTREE POUR AVANCE"] {:vars {} :funs {}}] (parse-atomic-cmd ["AVANCE"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-parse-a-command-block
  (testing "parse-cmd-block should parse a command block"
    (is (= [[:recule [5] :levecrayon [] []] {:vars {} :funs {}}] (parse-cmd-block ["[" "RECULE" "5" "LC" "]"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-return-remaining-words
  (testing "parse-cmd-block should return remaining words"
    (is (= [[:baissecrayon [] :droite [45] ["remaining" "words"]] {:vars {} :funs {}}] (parse-cmd-block ["[" "BC" "DROITE" "45" "]" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-check-for-opening-bracket
  (testing "parse-cmd-block should check for opening bracket"
    (is (= [:no-match {:vars {} :funs {}}] (parse-cmd-block ["RECULE" "5" "LC" "]"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-check-for-closing-bracket
  (testing "parse-cmd-block should check for closing bracket"
    (is (= [[:error "PAS DE CROCHET FERMANT"] {:vars {} :funs {}}] (parse-cmd-block ["[" "RECULE" "5" "LC" "babibubebo"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-return-incomplete-if-no-closing-bracket-and-no-remaining-words
  (testing "parse-cmd-block should return :incomplete if no closing bracket and no remaining words"
    (is (= [:incomplete {:vars {} :funs {}}] (parse-cmd-block ["[" "RECULE" "5" "LC"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-return-nested-error
  (testing "parse-cmd-block should return nested error"
    (is (= [[:error "PAS ASSEZ D'ENTREE POUR AV"] {:vars {} :funs {}}] (parse-cmd-block ["[" "RECULE" "2" "AV"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-return-incomplete-if-nested-cmd-returns-incomplete
  (testing "parse-cmd-block should return :incomplete if nested command returns :incomplete"
    (is (= [:incomplete {:vars {} :funs {}}] (parse-cmd-block ["[" "RECULE" "5" "REPETE" "2" "[" "AV" "10"] {:vars {} :funs {}})))))

(deftest parse-cmd-block-should-not-propagate-variable-assignments
  (testing "parse-cmd-block should not propagate variable assignments"
    (is (= [[:avance [10] []] {:vars {} :funs {}}] (parse-cmd-block ["[" "DONNE" "DIST" "10" "AV" "DIST" "]"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-parse-a-repeat-command
  (testing "parse-repeat-cmd should parse a repeat command"
    (is (= [[:avance [10] :droite [90] :avance [10] :droite [90] :avance [10] :droite [90] :avance [10] :droite [90] []] {:vars {} :funs {}}] (parse-repeat-cmd ["REPETE" "4" "[" "AV" "10" "DROITE" "90" "]"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-return-remaining-words
  (testing "parse-repeat-cmd should return remaining words"
    (is (= [[:avance [10] :droite [90] :avance [10] :droite [90] ["remaining" "words"]] {:vars {} :funs {}}] (parse-repeat-cmd ["REPETE" "2" "[" "AV" "10" "DROITE" "90" "]" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-return-no-match-if-not-a-repeat-command
  (testing "parse-repeat-cmd should return :no-match if not a repeat command"
    (is (= [:no-match {:vars {} :funs {}}] (parse-repeat-cmd ["AV" "10" "REPETE" "2" "[" "TD" "90" "]"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-check-repeat-argument
  (testing "parse-repeat-cmd should check the repeat command argument"
    (is (= [[:error "MAUVAIS PARAMETRE POUR REPETE"] {:vars {} :funs {}}] (parse-repeat-cmd ["REPETE" "ABC" "[" "TD" "90" "]"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-return-incomplete-if-the-nested-block-is-incomplete
  (testing "parse-repeat-cmd should return :incomplete if the nested block is incomplete"
    (is (= [:incomplete {:vars {} :funs {}}] (parse-repeat-cmd ["REPETE" "25" "[" "TD" "90"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-return-nested-error
  (testing "parse-repeat-cmd should return nested error"
    (is (= [[:error "MAUVAIS PARAMETRE POUR TG"] {:vars {} :funs {}}] (parse-repeat-cmd ["REPETE" "3" "[" "TG" "ABC" "]"] {:vars {} :funs {}})))))

(deftest parse-repeat-cmd-should-accept-variable-for-argument
  (testing "parse-repeat-cmd should accept variable for argument"
    (is (= [[:avance [10] :droite [90] :avance [10] :droite [90] []] {:vars {"TOTAL" 2} :funs {}}] (parse-repeat-cmd ["REPETE" "TOTAL" "[" "AV" "10" "DROITE" "90" "]"] {:vars {"TOTAL" 2} :funs {}})))))

(deftest parse-repeat-cmd-should-accept-variable-in-repeat-body
  (testing "parse-repeat-cmd should accept variable in repeat body"
    (is (= [[:avance [10] :droite [90] :avance [10] :droite [90] []] {:vars {"DISTANCE" 10} :funs {}}] (parse-repeat-cmd ["REPETE" "2" "[" "AV" "DISTANCE" "DROITE" "90" "]"] {:vars {"DISTANCE" 10} :funs {}})))))

(deftest parse-assign-cmd-should-parse-an-assignment
  (testing "parse-assign-cmd should parse an assignment"
    (is (= [[[]] {:vars {"LONGUEUR" 5} :funs {}}] (parse-assign-cmd ["DONNE" "LONGUEUR" "5"] {:vars {} :funs {}})))))

(deftest parse-assign-cmd-should-accept-only-valid-variable-names
  (testing "parse-assign-cmd should parse an assignment"
    (is (= [[:error "NE PEUX APPELER UNE VARIABLE 1"] {:vars {} :funs {}}] (parse-assign-cmd ["DONNE" "1" "5"] {:vars {} :funs {}})))))

(deftest parse-assign-cmd-should-return-remaining-words
  (testing "parse-assign-cmd should return remaining words"
    (is (= [[["remaining" "words"]] {:vars {"X" 2} :funs {}}] (parse-assign-cmd ["DONNE" "X" "2" "remaining" "words"] {:vars {} :funs {}})))))

(deftest parse-assign-cmd-should-check-assigned-value
  (testing "parse-assign-cmd should check assigned value"
    (is (= [[:error "MAUVAIS PARAMETRE POUR DONNE"] {:vars {} :funs {}}] (parse-assign-cmd ["DONNE" "Y" "BLABLA"] {:vars {} :funs {}})))))

(deftest parse-param-should-parse-a-single-parameter
  (testing "parse-param should parse a single parameter"
    (is (= [":Param1" []] (parse-param [":Param1"])))))

(deftest parse-param-should-only-match-a-valid-parameter
  (testing "parse-param should only match a valid parameter"
    (is (= :no-match (parse-param ["invalid"])))
    (is (= :no-match (parse-param ["in:valid"])))
    (is (= :no-match (parse-param [":invalid!"])))))

(deftest parse-param-should-return-remaining-words
  (testing "parse-param should return remaining words"
    (is (= [":abc" ["remaining" "words"]] (parse-param [":abc" "remaining" "words"])))))

(deftest parse-params-should-parse-multiple-parameters-and-return-remaining-words
  (testing "parse-params should parse multiple parameters and return remaining words"
    (is (= [[":Param1" ":hello" ":world"] ["remaining" ":words"]] (parse-params [":Param1" ":hello" ":world" "remaining" ":words"])))))

(deftest valid-name-should-return-true-if-word-is-a-valid-function-name
  (testing "valid-name? should return true if word is valid function name"
    (is (false? (valid-name? nil)))
    (is (false? (valid-name? "POUR")))
    (is (false? (valid-name? ":ABC")))
    (is (true? (valid-name? "CARRE")))
    (is (false? (valid-name? "SOMME")))))

(deftest parse-function-declaration-should-parse-a-function-declaration
  (testing "parse-function-declaration should parse a function declaration"
    (is (= [[[]] {:vars {} :funs {"CARRE" [[":COTE"] ["AV" ":COTE" "DROITE" "90"]]}}] (parse-function-declaration ["POUR" "CARRE" ":COTE" "[" "AV" ":COTE" "DROITE" "90" "]"] {:vars {} :funs {}})))
    (is (= [[[]] {:vars {} :funs {"CARRE" [[":COTE"] ["AV" ":COTE" "DROITE" "90"]]}}] (parse-function-declaration ["POUR" "CARRE" ":COTE" "AV" ":COTE" "DROITE" "90" "FIN"] {:vars {} :funs {}})))
    (is (= [[[]] {:vars {} :funs {"CARRE" [[] ["AV" "10" "DROITE" "90"]]}}]  (parse-function-declaration ["POUR" "CARRE" "AV" "10" "DROITE" "90" "FIN"] {:vars {} :funs {}})))
    (is (= [[[]] {:vars {} :funs {"CARRE" [[":X" ":Y"] ["AV" ":X" "DROITE" ":Y"]]}}]  (parse-function-declaration ["POUR" "CARRE" ":X" ":Y" "AV" ":X" "DROITE" ":Y" "FIN"] {:vars {} :funs {}})))))

(deftest parse-function-declaration-should-check-start-and-end
  (testing "parse-function-declaration should check start and end"
    (is (= [[:error "PAS DE FIN A LA FONCTION CARRE"] {:vars {} :funs {}}]  (parse-function-declaration ["POUR" "CARRE" ":COTE" "AV" ":COTE" "DROITE" "90" "BLABLA"] {:vars {} :funs {}})))
    (is (= [[:error "PAS DE CROCHET FERMANT POUR CARRE"] {:vars {} :funs {}}]  (parse-function-declaration ["POUR" "CARRE" ":COTE" "[" "AV" ":COTE" "DROITE" "90" "BLABLA"] {:vars {} :funs {}})))
    (is (= [:incomplete {:vars {} :funs {}}]  (parse-function-declaration ["POUR" "CARRE" ":COTE" "AV" ":COTE" "DROITE" "90"] {:vars {} :funs {}})))
    (is (= [:no-match {:vars {} :funs {}}]  (parse-function-declaration ["AVEC" "CARRE" ":COTE" "AV" ":COTE" "FIN"] {:vars {} :funs {}})))
    (is (= [[[]] {:vars {} :funs {"LIGNE" [[] ["REPETE" "2" "[" "AV" "5" "]"]]}}] (parse-function-declaration ["POUR" "LIGNE" "[" "REPETE" "2" "[" "AV" "5" "]" "]"] {:vars {} :funs {}})))))

(deftest parse-function-declaration-should-accept-empty-function
  (testing "parse-function-declaration should accept empty function"
    (is (= [[[]] {:vars {} :funs {"CARRE" [[] []]}}]  (parse-function-declaration ["POUR" "CARRE" "FIN"] {:vars {} :funs {}})))))

(deftest parse-function-declaration-should-check-function-name
  (testing "parse-function-declaration should check function name"
    (is (= [[:error "PAS ASSEZ D'ENTREE POUR POUR"] {:vars {} :funs {}}]  (parse-function-declaration ["POUR"] {:vars {} :funs {}})))
    (is (= [[:error "NE PEUX APPELER UNE FONCTION DONNE"] {:vars {} :funs {}}]  (parse-function-declaration ["POUR" "DONNE" ":X" "AV" "FIN"] {:vars {} :funs {}})))))

(deftest parse-function-declaration-should-accept-nested-repeat-command-with-argument
  (testing "parse-function-declaration should accept nested repeat command with argument"
    (is (= [[[]] {:vars {} :funs {"POLYGONE" [[":X"] ["REPETE" ":X" "[" "AV" "10" "DROITE" "90" "]"]]}}]  (parse-function-declaration ["POUR" "POLYGONE" ":X" "REPETE" ":X" "[" "AV" "10" "DROITE" "90" "]" "FIN"] {:vars {} :funs {}})))))

(deftest parse-function-declaration-should-accept-nested-expression-containing-variable
  (testing "parse-function-declaration should accept nested expression containing variable"
    (is (= [[[]] {:vars {} :funs {"POLYGONE" [[":N" ":COTE"] ["REPETE" ":N" "[" "AV" ":COTE" "DROITE" "360" "/" ":N" "]"]]}}]  (parse-function-declaration ["POUR" "POLYGONE" ":N" ":COTE" "REPETE" ":N" "[" "AV" ":COTE" "DROITE" "360" "/" ":N" "]" "FIN"] {:vars {} :funs {}})))))

(deftest parse-function-invocation-should-parse-a-function-invocation
  (testing "parse-function-invocation should parse a function invocation"
    (is (= [[:avance [10] :droite [70] []] {:vars {} :funs {"ANGLE" [[":A"] ["AV" "10" "DROITE" ":A" "]"]]}}]  (parse-function-invocation ["ANGLE" "70"] {:vars {} :funs {"ANGLE" [[":A"] ["AV" "10" "DROITE" ":A" "]"]]}})))))

(deftest parse-show-cmd-should-parse-a-show-command
  (testing "parse-show-cmd should parse a show command"
    (is (= [[:ecris ["32.0"] []] {:vars {"ABC" 32} :funs {}}] (parse-show-cmd ["MONTRE" "ABC"]  {:vars {"ABC" 32} :funs {}})))
    (is (= [[:error "PAS ASSEZ D'ENTREE POUR ECRIS"] {:vars {} :funs {}}] (parse-show-cmd ["ECRIS"] {:vars {} :funs {}})))
    (is (= [[:error "NE SAIS COMMENT FAIRE ABC"] {:vars {} :funs {}}] (parse-show-cmd ["ECRIS" "ABC"] {:vars {} :funs {}})))))

(deftest parse-single-cmd-should-parse-an-atomic-command
  (testing "parse-single-cmd should parse an atomic command"
    (is (= [[:avance [10] []] {:vars {} :funs {}}] (parse-single-cmd ["AV" "10"] {:vars {} :funs {}})))))

(deftest parse-single-cmd-should-parse-a-repeat-command
  (testing "parse-single-cmd should parse a repeat command"
    (is (= [[:droite [90] :droite [90] []] {:vars {} :funs {}}] (parse-single-cmd ["REPETE" "2" "[" "TD" "90" "]"] {:vars {} :funs {}})))))

(deftest parse-single-cmd-should-parse-a-repeat-command-with-variable
  (testing "parse-single-cmd should parse a repeat command with variable"
    (is (= [[:avance [10] :droite [90] :avance [10] :droite [90] []] {:vars {"REPETITIONS" 2 "DISTANCE" 10} :funs {}}] (parse-single-cmd ["REPETE" "REPETITIONS" "[" "AV" "DISTANCE" "DROITE" "90" "]"] {:vars {"REPETITIONS" 2 "DISTANCE" 10} :funs {}})))))

(deftest parse-single-cmd-should-parse-an-assignment
  (testing "parse-single-cmd should parse an assignment"
    (is (= [[[]] {:vars {"LOLO" 99} :funs {}}] (parse-single-cmd ["DONNE" "LOLO" "99"] {:vars {} :funs {}})))))

(deftest parse-single-cmd-should-parse-a-function-declaration
  (testing "parse-single-cmd should parse a function declaration"
    (is (= [[[]] {:vars {} :funs {"POLYGONE" [[":X"] ["REPETE" ":X" "[" "AV" "10" "DROITE" "90" "]"]]}}]  (parse-single-cmd ["POUR" "POLYGONE" ":X" "REPETE" ":X" "[" "AV" "10" "DROITE" "90" "]" "FIN"] {:vars {} :funs {}})))))

(deftest parse-single-cmd-should-parse-a-function-invocation
  (testing "parse-single-cmd should parse a function invocation"
    (is (= [[:avance [10] :droite [120] :avance [10] :droite [120] :avance [10] :droite [120] []] {:vars {} :funs {"POLYGONE" [[":N" ":COTE"] ["REPETE" ":N" "[" "AV" ":COTE" "DROITE" "360" "/" ":N" "]"]]}}]  (parse-single-cmd ["POLYGONE" "3" "10"] {:vars {} :funs {"POLYGONE" [[":N" ":COTE"] ["REPETE" ":N" "[" "AV" ":COTE" "DROITE" "360" "/" ":N" "]"]]}})))))

(deftest parse-single-cmd-should-parse-a-show-command
  (testing "parse-single-cmd should parse a show command"
    (is (= [[:ecris ["[0.0 1.0]"] []] {:vars {"POS" [0.0 1.0]} :funs {}}] (parse-single-cmd ["ECRIS" "POS"]  {:vars {"POS" [0.0 1.0]} :funs {}})))))

(deftest parse-multiple-cmd-should-parse-multiple-commands
  (testing "parse-multiple-cmd should parse multiple commands"
    (is (= [[:avance [5] :droite [90] :droite [90] []] {:vars {} :funs {}}] (parse-multiple-cmd ["AV" "5" "REPETE" "2" "[" "TD" "90" "]"] {:vars {} :funs {}})))))

(deftest parse-multiple-cmd-should-parse-assignment
  (testing "parse-multiple-cmd should parse multiple commands"
    (is (= [[:droite [90] :avance [4] []] {:vars {"ANGLE" 90 "LONGUEUR" 4} :funs {}}] (parse-multiple-cmd ["TD" "ANGLE" "DONNE" "LONGUEUR" "4" "AV" "LONGUEUR"] {:vars {"ANGLE" 90} :funs {}})))))

(deftest parse-multiple-cmd-should-parse-a-function-declaration
  (testing "parse-multiple-cmd should parse a function declaration"
    (is (= [[:baissecrayon [] :gauche [90] :avance [50] []] {:vars {} :funs {"POLYGONE" [[":X"] ["REPETE" ":X" "[" "AV" "10" "DROITE" "90" "]"]]}}]  (parse-multiple-cmd ["BC" "GAUCHE" "90" "POUR" "POLYGONE" ":X" "REPETE" ":X" "[" "AV" "10" "DROITE" "90" "]" "FIN" "AV" "50"] {:vars {} :funs {}})))
    (is (= [[:avance [5] :avance [5] []] {:vars {} :funs {"LIGNE" [[] ["REPETE" "2" "[" "AV" "5" "]"]]}}] (parse-multiple-cmd ["POUR" "LIGNE" "[" "REPETE" "2" "[" "AV" "5" "]" "]" "LIGNE"] {:vars {} :funs {}})))))
