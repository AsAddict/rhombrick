(ns rhombrick.jigsaw
  (:use [scad-clj.scad]
        [scad-clj.model]
        [rhombrick.tiling]
        [rhombrick.tilecode]
        [rhombrick.staticgeometry :as geom]
        [rhombrick.tile-shape-2d]
        [rhombrick.tiling-render :only [get-tile-color]]
        [rhombrick.vector]
        ))


(reset! current-topology (topologies :hexagon))


(def piece
  (->> (make-jigsaw-piece "4-4---" @current-topology 32)
       (apply concat)
       (map #(vec [(% 0) (% 1)]))
       (vec)))


(defn get-piece-verts-2d [code topo res ]
  (->> (make-jigsaw-piece code topo res)
       (apply concat)
       (map #(vec [(% 0) (% 1)])) ; make 2d
       (vec)))


(defn do-tiling  [ts topo]
  (let [tiles (ts :tiles)
        tile-fn (fn [pos code]
                  (color (vec3-scale (get-tile-color code) (/ 1.0 255.0))
                         (translate pos
                    (scale [15 15 0]
                      ;(extrude-linear {:height 0.25}
                        (polygon (get-piece-verts-2d code topo 8))
                       ;)
                             ))))]
    (map #(tile-fn (key %) (val %)) tiles)))
    ;(union (map #(tile-fn (key %) (val %)) tiles))))


(defn make-sheet [width-mm height-mm nrows ncols tile-radius-mm margin]
  (let [tiles (into {} (map #(vec [%1 (make-normal-random-tilecode)])
                            (apply concat
                              (for [j (range nrows)]
                                (for [i (range ncols)]
                                  (let [offset (* (mod i 2) 0.5)
                                        x (+ margin
                                             (* i tile-radius-mm))
                                        y (+ margin
                                             (* offset tile-radius-mm)
                                             (* j tile-radius-mm))]
                                    [x y]))))))]
    (do-tiling {:tiles tiles} @current-topology)
    ))


(def sheet (make-sheet 600 300 10 20 40 5))

(def tile
  ;(difference
    (color [1.0 0.0 0.0]
      (scale [20 20 0]
      ;(extrude-linear {:height 0.2}
             ;(sphere 10)
             (polygon piece)
      ;       )
           ))
    ;                )
    ;(with-fs 0.01 (cylinder 0.45 0.5))
  ; )
  )


(def tiles (do-tiling @tiler-state @current-topology))

;tiles

;(spit "tiles.scad" (write-scad tiles))
(spit "sheet.scad" (write-scad sheet))
(spit "piece.scad" (write-scad tile))


;(set-tileset ["--CC-C" "2-CC-C" "-2CC-C" "c-c-c-" "2-2---" "22----" "2-2-2-"])