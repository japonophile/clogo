(ns clogo.core-nxt
  (:require [clogo.core :refer [app-init nxt-exec status-bar ui]]
            [clogo.java.bluetooth :refer :all]
            [clogo.util :refer [call-after]]
            [seesaw.core :refer [text! pack! show!]])
  (:gen-class))

;; Bluetooth interface to NXT

(defn nxt-init []
  (add-watch bt-agent :bt-status
             (fn [ag key oldstatus newstatus]
               (text! status-bar
                      (case (first newstatus)
                        :disconnected "Pas de connexion à la tortue"
                        :connecting   "Connexion en cours..."
                        :connected    "Connecté à la tortue"
                        "???"))))

  (intern 'clojure.core 'nxt-exec
          (fn [action args] (bt-exec action args)))

  (call-after 5000 bt-connect))

;; Init the application, enable bluetooth and display the main UI

(defn -main [& args]
  (app-init)
  (nxt-init)
  (-> ui pack! show!))

