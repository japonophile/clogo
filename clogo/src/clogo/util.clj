(ns clogo.util)

(defn- replace-numbered-arg
  [fmt idx & args]
  (if (nil? args)
    fmt
    (let [newstr (clojure.string/replace fmt (str "{" idx "}") (str "" (first args)))]
      (apply replace-numbered-arg (into [newstr (inc idx)] (rest args))))))

(defn formatstr
  "Formats a string by including arguments in the specified order."
  [fmt & args]
  (apply replace-numbered-arg (into [fmt 1] args)))

(defn call-after
  "Calls a function with given arguments after <ms> milliseconds."
  [ms f & args]
  (future
    (do
      (Thread/sleep ms)
      (apply f args))))

(defn vdiv
  "Divides each element of vector <v> by divider <d>"
  [v d]
  {:pre [(not= 0 d)]}
  (vec (map #(/ % d) v)))

(defn round
  "Rounds a value <x> with a given <precision>."
  [x precision]
  (if (vector? x)
    (vec (map #(round % precision) x))
    (-> x (bigdec)
      (.movePointRight precision)
      (+ 0.5) (int) (bigdec)
      (.movePointLeft precision)
      (double))))

