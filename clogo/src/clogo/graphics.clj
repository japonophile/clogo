(ns clogo.graphics)

(defprotocol TwoDGraphics
  "2D graphic primitives."
  (canvas         [g width height]                     "Creates a canvas of give <width> and <height>.")
  (canvas-panel   [g paint-panel]                      "Creates a panel which will be painted by calling
                                                        the supplied <paint-panel> method passing as 
                                                        argument the graphics to draw on.")
  (draw-polygon!  [g canvas width height color points] "Draws a polygon linking all <points> 
                                                        on a <canvas> (which can also be a graphics)
                                                        with given <width> and <height> using
                                                        the supplied <color>.")
  (draw-canvas!   [g graphics canvas]                  "Draws a <canvas> on graphics <g>.")
  (clear-canvas!  [g canvas]                           "Clears a <canvas>.")
  (repaint-panel! [g panel]                            "Forces a <panel> to be repainted.")
  (green          [g]                                  "Represents the green color."))
