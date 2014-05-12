(ns clogo.core
  (:require [clojure.string :refer [join]]
            [seesaw.core :refer :all]
            [seesaw.mig :refer :all]
            [clogo.parser :refer :all]
            [clogo.memory :refer :all]
            [clogo.turtle :refer :all]
            [clogo.executor :refer :all]
            [clogo.console :refer :all]
            [clogo.java.graphics :refer :all]
            [clogo.java.io :refer :all])
  (:import  (javax.swing JFrame JLabel)
            (clogo.java.graphics Java2DGraphics))
  (:gen-class))

(defn app-init
  "Initialize the LOGO application"
  []

  (def console-font "Courier New")

  (def graphic-commands
    {
     :nettoie      cleanup!
     :montretortue show-turtle!
     :cachetortue  hide-turtle!
     :avance       forward!
     :recule       back!
     :gauche       left!
     :droite       right!
     :levecrayon   pen-up!
     :baissecrayon pen-down!
    })

  ;; Turtle agent (to draw on the screen)

  (def turtle-agent (turtle (Java2DGraphics.) 400 400))

  (def exec-agent (executor turtle-agent graphic-commands))

  (add-watch turtle-agent :turtlevars
             (fn [ag key oldstatus newstatus]
               (send mem defvar "POS" (pos newstatus))
               (send mem defvar "CAP" (heading newstatus))))

  ;; Dummy NXT executor (will be enabled only on demand)

  (defn nxt-exec [action args] nil)

  ;; To execute commands that output to the console

  (defn text-exec
    [action args]
    (if (= :ecris action)
      (str (first args) "\n")
      ""))

  ;; Command execution: dispatch to NXT (optional), turtle and console

  (defn exec-cmds [commands]
    (reduce str (for [[action args] (partition 2 commands)]
                  (do
                    (nxt-exec action args)
                    (turtle-exec exec-agent action args)
                    (text-exec action args)))))

  (def console-agent
    (console (str console-font "-bold-24") parse-input exec-cmds (read-msg)))

  (add-watch console-agent :messages
             (fn [ag key oldst newst]
               (write-msg! (subvec (:messages newst) (count (:messages oldst))))))

  ;; Status bar

  (def status-bar
    (label :font (str console-font "--12")
           :text "Pas de connexion à la tortue"))

  ;; Variable and function panels

  (def mem-vars-panel
    (text :multi-line? true
          :font (str console-font "--18")
          :editable? false))

  (def mem-funs-panel
    (text :multi-line? true
          :font (str console-font "--18")
          :editable? false))

  (add-watch mem :display
             (fn [ag key oldstatus newstatus]
               (text! mem-vars-panel (join "\n" (keys (newstatus :vars))))
               (text! mem-funs-panel (join "\n" (keys (newstatus :funs))))))

  ;; Main windows

  (def ui
    (frame :title "LOGO"
           :on-close :exit
           :content
           (mig-panel
             :constraints ["fill,insets 5"
                           "[fill,:200:]5[fill,:200:]5[center,:400:]"
                           "[]5[fill,100!]5[center]"]
             :items [[status-bar "span"]
                     [mem-vars-panel]
                     [mem-funs-panel]
                     [(turtle-panel turtle-agent) "span 1 2,h :400:,w :400:,wrap"]
                     [(scrollable (:area @console-agent)) "span 2 1,growy,h :295:"]])))
)

;; Init the application and display the main UI

(defn -main [& args]
  (app-init)
  (-> ui pack! show!))

