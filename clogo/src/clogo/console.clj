(ns clogo.console
  (:require [clogo.java.ui :refer :all]))

(defn min-cursor!
  "Updates min cursor."
  [console n]
  (send console #(assoc % :min-cursor n)))

(defn inc-min-cursor!
  "Increments min cursor of a given <offset>."
  [console offset]
  (send console #(assoc % :min-cursor (+ (:min-cursor %) offset))))

(defn set-cursor!
  "Sets the cursor in the console, and adjust min cursor."
  [console n]
  (cursor! (:area @console) n)
  (min-cursor! console n))

(defn add-msg!
  "Adds a message <msg> with a given <type> (:s for system message
   and :u for user input)."
  [console type msg]
  (send console #(assoc % :messages (conj (:messages %) [type msg]))))

(defn log-msg!
  "Logs a message in the console."
  [console msg]
  (if-let [area (:area @console)]
    (let [prompt (:prompt @console)
          old-text (get-text area)
          new-text (str old-text msg prompt)
          end-of-buffer (count new-text)]
      (do
        (set-text! area new-text)
        (add-msg! console :s msg)
        (set-cursor! console end-of-buffer)))))

(defn process-input
  "Processes user input from the console."
  [console user-input]
  (let [parse-input (:parse-input-fn @console)
        exec-cmds (:exec-cmds-fn @console)
        parse-result (parse-input user-input)]
    (if (not= :incomplete parse-result)
      (do
        (inc-min-cursor! console (count user-input))
        (add-msg! console :u user-input)
        (if (= :error (first parse-result))
          (log-msg! console (str (second parse-result) "\n"))
          (log-msg! console (exec-cmds parse-result)))))))

(defn process-keypressed
  "Returns a function that will process key pressed in the console."
  [console]
  (fn [c]
    (let [cursor (cursor (:area @console))
          min-cursor (:min-cursor @console)]
      (if (<= cursor min-cursor)
        (do
          (if (not= (char 65535) c) ;; Allow CMD key (on Mac) to copy text (on PC: ??)
            (set-cursor! console min-cursor))
          (not= \backspace c)) ;; return false (stop processing) for backspace when cursor is at min-cursor
        true)))) ;; return true (process key event normally)

(defn process-keytyped
  "Returns a function that will process key typed in the console."
  [console]
  (fn [c]
    (let [area (:area @console)
          min-cursor (:min-cursor @console)]
      (if (= c \newline)
        (let [user-input (subs (get-text area) min-cursor)]
          (process-input console user-input))))))

(defn init-area
  "Initialize the console text area with existing messages."
  [console msgs]
  (let [prompt (:prompt @console)]
    (if (empty? msgs)
      (log-msg! console "")
      (let [txt (str (apply str (map #(if (= :u (first %))
                                        (str prompt (second %))
                                        (second %)) msgs)) prompt)]
        (set-text! (:area @console) txt)
        (set-cursor! console (count txt))))))

(defn console
  "Constructs a console (agent) with given <font>,
   and registers functions: <parse-input> to parse user input
   and <exec-cmds> to execute commands typed in the console."
  [font parse-input exec-cmds msgs]
  (let [console-agent (agent {:min-cursor 0 :prompt "> " :messages msgs
                              :parse-input-fn parse-input :exec-cmds-fn exec-cmds})]
    (send console-agent #(assoc % :area (text-area font
                                                   (process-keypressed console-agent)
                                                   (process-keytyped console-agent))))
    (await console-agent)
    (init-area console-agent msgs)
    console-agent))
