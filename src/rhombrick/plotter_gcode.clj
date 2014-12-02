(ns rhombrick.plotter-gcode
  (:use [rhombrick.vector])
  )

(def max-draw-radius 130.0)

(def z-lift 5.0)
(def draw-speed-mms 30.0)
(def travel-speed-mms 150.0)

(def draw-speed (* draw-speed-mms 60.0))
(def travel-speed (* travel-speed-mms 60.0))


(defn set-speed [speed] (format "G1 F%.4f" speed))
(defn pen-up [] (format "G1 Z%.4f F%.4f" z-lift travel-speed))
(defn pen-down [] (format "G1 Z%.4f F%.4f" 0.0 travel-speed))


(defn move-to
  ([x y]
    (format "G1 X%.4f Y%.4f" (float x) (float y)))
  ([[x y z]]
    (format "G1 X%.4f Y%.4f Z%.4f" (double x) (double y) (double z))))


(defn clip [vert]
  (if (> (vec3-length vert) max-draw-radius)
    (vec3-scale (vec3-normalize vert) max-draw-radius)
    vert))

; takes a collection of strings and joins them with newlines into a single string
(defn gcodify [col] (apply str (interpose "\n" col)))


; output gcode to draw a sequence of lines defined by a sequence of points.
; the z coordinate will be ignored.
(defn line-sequence [points]
  (let [verts (map clip points)]
    (gcodify [
      (set-speed travel-speed)
      (move-to [((first verts) 0) ((first verts) 1) z-lift]) ; above first point
      (pen-down)  ; touch paper at first point
      (set-speed draw-speed)
      (gcodify
        (map #(move-to (% 0) (% 1)) (rest verts))) ; draw the verts
      (pen-up) ; lift pen 
      ]
)))


(def wait-tool-change
  (gcodify [(set-speed travel-speed)
            (move-to [0.0 0.0 200.0])
            "G4 P60000"]))
            
;["G1 Z200 F7800\n"] ; move up 200mm
;["M117 change pen\n"]
;["M226\n"] ; gcode initiated pause


