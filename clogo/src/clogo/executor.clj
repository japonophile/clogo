(ns clogo.executor)

(defn executor
  "Returns an executor agent, capable of executing commands
   on the <turtle>.  It requires a map of available <commands>."
  [turtle commands]
  (agent {:turtle turtle :commands commands
          :animate true :animation-delay 50
          :state :ready :cmd-queue []}))

(declare exec-animated exec-directly)

(defn exec-cmd
  "Executes a turtle command (either animated or directly)."
  [ex exec-ag cmd [arg]]
  (let [s (:step (meta cmd))]
    (if (and (:animate ex) s)
      (exec-animated ex exec-ag cmd arg s)
      (exec-directly ex exec-ag cmd arg))))

(defn exec-or-queue-cmd
  "Executes a command with arguments or queue it if the turtle is busy."
  [ex exec-ag cmd args]
  (if (= :ready (:state ex))
    (exec-cmd ex exec-ag cmd args)
    (assoc ex :cmd-queue (conj (:cmd-queue ex) [cmd args]))))

(defn end-cmd
  "Called at the end of a command, checks if any command is queued and runs it;
   otherwise, set turtle state back to :ready."
  [ex exec-ag]
  (let [[[cmd args]] (:cmd-queue ex)]
    (if (nil? cmd)
      (assoc ex :state :ready)
      (let [newex (assoc ex :cmd-queue (vec (rest (:cmd-queue ex))))]
        (exec-cmd newex exec-ag cmd args)))))

(defn send-end-cmd [exec-ag] (send exec-ag (fn [ex] (end-cmd ex exec-ag))))

(defn exec-directly
  "Execute a command directly (not animated)."
  [ex exec-ag cmd arg]
  (let [turtle (:turtle ex)]
    (if arg
      (send turtle cmd arg)
      (send turtle cmd))
    (if (= :busy (:state ex))
      (future (send-end-cmd exec-ag)))
    ex))

(defn exec-animated
  "Starts an animation to perform an operation on the turtle step by step."
  [ex exec-ag cmd steps s]
  (let [turtle (:turtle ex)
        step (if (> steps 0) s (- s))
        times (quot steps step)
        last (rem steps s)]
    (future
      (doseq [f (repeat times #(cmd % step))]
        (Thread/sleep (:animation-delay ex))
        (send turtle f))
      (if last (send turtle #(cmd % last)))
      (send-end-cmd exec-ag))
    (assoc ex :state :busy)))

(defn turtle-exec
  "Requests the graphic turtle to execute a command with some arguments."
  [exec-ag action args]
  (if-let [cmd ((:commands @exec-ag) action)]
    (send exec-ag (fn [ex] (exec-or-queue-cmd ex exec-ag cmd args)))
    (condp = action
      :aupas   (send exec-ag #(assoc % :animate true :animation-delay 200))
      :autrot  (send exec-ag #(assoc % :animate true :animation-delay 50))
      :augalop (send exec-ag #(assoc % :animate false))
      nil)))

