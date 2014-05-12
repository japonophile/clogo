(ns clogo.memory)

(def mem (agent { :vars {} :funs {} }))

(defn defvar [mem variable value]
  (assoc mem :vars (assoc (mem :vars) variable value)))

(defn deffun [mem function body]
  (assoc mem :funs (assoc (mem :funs) function body)))

(defn reset-mem! []
  (send mem
        (fn [m]
          { :vars {} :funs {} })))

(defn update-mem! [newmem]
  (send mem
        (fn [oldmem]
          newmem)))
