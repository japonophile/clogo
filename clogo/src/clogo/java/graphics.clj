(ns clogo.java.graphics
  (:require [clogo.graphics :refer :all])
  (:import  (javax.swing JLabel)
            (java.awt.image BufferedImage)
            (java.awt Dimension Color)))

(deftype Java2DGraphics []
  TwoDGraphics

  (canvas         [g width height]
    (BufferedImage. width height BufferedImage/TYPE_INT_RGB))

  (canvas-panel   [g paint-panel]
    (proxy [JLabel] [] 
      (paint [graphics] (paint-panel graphics))))

  (draw-polygon!  [g canvas width height color points]
    (let [graphics (if (instance? java.awt.Graphics canvas) canvas (.getGraphics canvas))]
      (doto graphics
        (.translate (/ width 2) (/ height 2))
        (.scale 1.0 -1.0)
        (.setColor color))
      (doseq [[p1 p2] (map list (butlast points) (rest points))]  ;; [1 2 3 4] -> ((1 2) (2 3) (3 4))
        (.drawLine graphics (first p1) (second p1) (first p2) (second p2)))))

  (draw-canvas!   [g graphics canvas]
    (.drawImage graphics canvas 0 0 nil))

  (clear-canvas!  [g canvas]
    (doto (.getGraphics canvas)
      (.clearRect 0 0 (.getWidth canvas) (.getHeight canvas))))

  (repaint-panel! [g panel]
    (.repaint panel))

  (green          [g]
    Color/green))
