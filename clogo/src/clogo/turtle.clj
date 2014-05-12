(ns clogo.turtle
  (:require [clogo.graphics :refer :all]
            [clogo.util :refer [vdiv]]))

(def deg (/ (Math/PI) 180))

(defn turtle [g width height]
  (agent {:width width :height height :size 10 :scale 5
          :point [0 0] :angle 0 :color (green g)
          :graphics g
          :canvas (canvas g width height) :panel nil
          :drawing true :visible true}))

;; Calls to graphic primitives

(defn- draw-line!
  "Draws a line between <p1> and <p2>."
  [turtle p1 p2]
  (draw-polygon! (:graphics turtle) (:canvas turtle) (:width turtle) (:height turtle) (:color turtle) [p1 p2]))

(defn- draw-turtle!
  "Draws the turtle."
  [turtle graphics]
  (if (true? (:visible turtle))
    (let [p (:point turtle)
          angle (:angle turtle)
          size (:size turtle)
          s (* (Math/sin (* angle deg)) size)
          c (* (Math/cos (* angle deg)) size)
          p1 [(- (first p) (/ c 2)) (+ (second p) (/ s 2))]
          p2 [(+ (first p) s)       (+ (second p) c)]
          p3 [(+ (first p) (/ c 2)) (- (second p) (/ s 2))]
          width (:width turtle)
          height (:height turtle)]
    (draw-polygon! (:graphics turtle) graphics width height (:color turtle) [p1 p2 p3 p1]))))

(defn- turtle-updated [turtle key oldt t]
  (if-let [panel (:panel t)]
    (repaint-panel! (:graphics t) panel)))

(defn turtle-panel
  "Creates a panel associated with <turtle>."
  [turtle]
  (let [g (:graphics @turtle)
        panel (canvas-panel g 
                #(do (draw-canvas! g % (:canvas @turtle))
                   (draw-turtle! @turtle %)))]
    (send turtle #(assoc % :panel panel))
    (add-watch turtle :repaint turtle-updated)
    panel))

;; Turtle commands

(defn pen-up! 
  "Disable Drawing."
  [turtle]
  (assoc turtle :drawing false))

(defn pen-down! 
  "Enable Drawing."
  [turtle]
  (assoc turtle :drawing true))

(defn show-turtle!
  "Show the turtle."
  [turtle]
  (assoc turtle :visible true))

(defn hide-turtle!
  "Hide the turtle."
  [turtle]
  (assoc turtle :visible false))

(defn pen-color!
  "Set pen color"
  [turtle color]
  (assoc turtle :color color))

(def right!
  "Turn right through the angle <degrees>."
  ^{:step 5}
  (fn [turtle degrees]
    (let [angle (:angle turtle)
          heading (mod (+ angle degrees) 360)]
      (assoc turtle :angle heading))))

(def left!
  "Turn left through the angle <degrees>."
  ^{:step 5}
  (fn [turtle degrees]
    (right! turtle (- degrees))))

(def forward!
  "Move forward by <steps> turtle steps."
  ^{:step 1}
  (fn [turtle steps]
    (let [steps (* (:scale turtle) steps)
          p1 (:point turtle)
          angle  (:angle turtle)
          p2 [(+ (first p1) (* (Math/sin (* angle deg)) steps))
              (+ (second p1) (* (Math/cos (* angle deg)) steps))]]
      (if (true? (:drawing turtle)) (draw-line! turtle p1 p2))
      (assoc turtle :point p2))))

(def back!
  "Move backward by <steps> turtle steps."
  ^{:step 1}
  (fn [turtle steps]
    (forward! turtle (- steps))))

(defn cleanup!
  "Clean up the screen."
  [turtle]
  (clear-canvas! (:graphics turtle) (:canvas turtle))
  (repaint-panel! (:graphics turtle) (:panel turtle))
  turtle)

;; Functions to get info about the turtle

(defn pen-up? 
  "Is the pen up?"
  [turtle]
  (not (:drawing turtle)))

(defn turtle-visible? 
  "Is the turtle visible?"
  [turtle]
  (:visible turtle))

(defn pos
  "Returns the turtle current position."
  [turtle]
  (vdiv (:point turtle) (:scale turtle)))

(defn heading
  "Returns the angle towards were the turtle is heading."
  [turtle]
  (:angle turtle))
