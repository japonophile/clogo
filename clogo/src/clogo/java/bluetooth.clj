(ns clogo.java.bluetooth
  (:use [clogo.util :only [call-after]])
  (:import (java.lang.reflect Array)
           (java.io DataOutputStream)
           (lejos.pc.comm NXTComm NXTConnector NXTCommFactory)))

(def turtle-commands
  {
   :quitte       0
   :avance       1
   :recule       2
   :gauche       3
   :droite       4
   :levecrayon   5
   :baissecrayon 6
  })

(def bt-agent (agent [:disconnected]))

(defn bt-connect []
  (send bt-agent
        (fn [status]
          (if (= :disconnected (first status))
            [:connecting]
            status))))

(defn bt-write-cmd [status cmd]
  (case (first status)
    :connected (let [dos (DataOutputStream. (.getOutputStream (second status)))]
      (try
        (do
          (doseq [c cmd] (.writeInt dos c))
          (.flush dos)
          status)
        (catch Exception e
          (into [:connecting] cmd))))
    :connecting (into status cmd)
    :disconnected (into [:connecting] cmd)))

(defn bt-make-connection [ag key oldstatus newstatus]
  (if (and (not (= :connecting (first oldstatus)))
           (= :connecting (first newstatus)))
    (let [conn (NXTConnector.)
          devices (.search conn nil nil NXTCommFactory/BLUETOOTH)]
      (if (< 0 (Array/getLength devices))
        (do
          (println "connecting...")
          (if (.connectTo conn (Array/get devices 0) NXTComm/PACKET)
            (do
              (println "connected")
              (send bt-agent
                    (fn [status]
                      (if (= :connecting (first status))
                        (if (= [] (rest status))
                          [:connected conn]
                          (bt-write-cmd [:connected conn] (rest status)))
                        ;; should not happen
                        status))))
            ;; retry
            (do
              (println "connection failed")
              (call-after 5000 bt-make-connection ag key oldstatus newstatus))))
        (do
          (println "no bluetooth device found")
          (call-after 5000 bt-make-connection ag key oldstatus newstatus))))))

(add-watch bt-agent :make-connection bt-make-connection)

(defn bt-send [cmd]
  (send bt-agent bt-write-cmd cmd))

(defn bt-exec [action args]
  (let [turtle-cmd (turtle-commands action)]
    (if (not (nil? turtle-cmd))
      (bt-send (into [turtle-cmd] args)))))

;; for test only
(defn bt-reset []
  (send bt-agent
        (fn [status]
          [:disconnected])))
