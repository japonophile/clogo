(ns clogo.java.io
  (:import (java.io File FileWriter)))

(def clogodir "/.clogo")
(def msgfile "/messages.txt")

(defn outdir!
  "Returns the clogo output directory (creates is if it does not exist),
   or nil if the directory does not exist and cannot be created."
  []
  (let [home (System/getProperty "user.home")
        dirname (str home clogodir)
        dir (File. dirname)]
    (if (.exists dir)
      dirname
      (if (.exists (File. home))
        (do
          (.mkdirs dir)
          (if (.exists dir)
            dirname))))))

(defn write-msg!
  "Writes messages to disk."
  [msgs]
  (if (not (empty? msgs))
    (if-let [d (outdir!)]
      (binding [*out* (FileWriter. (str d msgfile) true)]
        (prn msgs)))))

(defn read-msg
  "Read messages from disk."
  []
  (let [fname (str (System/getProperty "user.home") clogodir msgfile)]
    (if (.exists (File. fname))
      (vec (apply concat (read-string (str "[" (slurp fname) "]"))))
      [])))
