(ns clogo.java.ui
  (:require [seesaw.core :refer :all]))

(defn cursor
  "Returns the cursor position in a text area."
  [area]
  (.getCaretPosition area))

(defn cursor!
  "Sets the cursor position in a text area."
  [area n]
  (.setCaretPosition area n))

(defn get-text
  "Returns the text of a text area."
  [area]
  (config area :text))

(defn set-text!
  "Sets the text of a text area."
  [area t]
  (text! area t))

(defn text-area
  "Creates a text area, with some <font> and with two callbacks:
   <process-keypressed>
   <process-keytyped>
   that will be called when a key is pressed, passing the key in argument.
   If the callback returns false, the key event will not be further processed."
  [font process-keypressed process-keytyped]
  (text :multi-line? true :font font
        :listen [:key (fn [event]
                        (let [c (.getKeyChar event)
                              type (.getID event)]
                          (if (= type java.awt.event.KeyEvent/KEY_TYPED)
                            (process-keytyped c)
                            (if (false? (process-keypressed c))
                              (.consume event)))))])) ;; interrupt event processing
