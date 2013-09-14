(ns rhombrick.tiling-render
  (:use [quil.core]
        [rhombrick.vector]
        [rhombrick.staticgeometry]
        ;[rhombrick.facecode]
        [rhombrick.tiling]
        [rhombrick.tilecode]
        [rhombrick.camera]
        [rhombrick.glider]
        [rhombrick.obj-loader]
        [rhombrick.bezierbox :as bbox]
        [clojure.math.combinatorics]))


(def rhomb-tex (atom nil))
(def current-tileset-colors (atom {}))
(def model-scale (atom 50))
;(def bezier-box-resolution (atom 8))
;(def bezier-box-control-bias (atom 0.5))
(def bezier-box-smooth-shading? (atom false))
(def bezier-box-line-weight (atom 2))
(def face-list (atom #{}))
(def face-id-text (atom []))



(defn draw-graph [[x y] w h title data-range data]
  (stroke-weight 1)
  (stroke 128 128 128 255)
  (fill 0 0 0 192)
  (rect x y w h)
  (stroke 192 192 255 255)
  (no-fill)
  (doseq [i (range (count data))]
    (if-let [d (data i)]
      (when (> d 0)
        (let [x1 (+ x i)
              y1 (+ y h)
              x2 (+ x i)
              y2 (- y1 (* (/ d data-range) h))]
          (point x2 y2))))))


;(defn get-graph-params []
;  [{:title "tiles"
;    :range ((@tiler-state :params) :max-tiles)
;    :data @stats-tile-count}
;   {:title "iter time"
;    :range 500
;    :data @stats-iter-time}
;   {:title "backtrack"
;    :range (max 100 (count @tiles))
;    :data @stats-backtrack}
;   {:title "efficiency"
;    :range 1.0
;    :data @stats-efficiency}
;  ])


;(defn draw-graphs [[x y]]
;  (let [graph-height 100
;        graph-space 5
;        graphs (get-graph-params)]
;    (doseq [i (range (count graphs))]
;      (let [graph (graphs i)
;            gx x
;            gy (+ y (* i graph-height) (* i graph-space))]
;        (draw-graph [gx gy]
;                    stats-buffer-length
;                    graph-height
;                    (graph :title)
;                    (graph :range)
;                    (graph :data))))))


(defn facecode-to-hex [code]
  (apply str "0x" (map #(if (= \- %) \0 %) code)))


(defn compute-tile-color [code]
  (if (not= nil code)
    (let [hs (facecode-to-hex code)
          n (mod (read-string hs) 12)]
      (rd-face-colors n))
    [128 128 128 128]))


(defn get-tile-color [code]
  (if (contains? @current-tileset-colors code)
    (@current-tileset-colors code)
    (do 
      (swap! current-tileset-colors assoc code (compute-tile-color code))
      (@current-tileset-colors code))))


(defn get-verts [verts face]
  (vec (map #(verts %) face)))

(defn get-obj-face-verts [obj]
  (->> (obj :face)
       (map #(get-verts (obj :vertex) %))
       (vec)))


;(def glider-model (load-obj "data/rhombic_dodecahedron.obj"))
(def glider-model (get-obj-face-verts (load-obj "data/smooth_spaceship.obj")))


(defn draw-obj [faces col]
;          [r g b a] col]
  (doseq [face faces]
    (let [nvs (count face)]
      (cond
        (= nvs 3)
          (begin-shape :triangles)
        (= nvs 4)
          (begin-shape :quads)
        :else
          (begin-shape))
      (doseq [v face]
        (vertex (v 0) (v 1) (v 2)))
      (end-shape))))


(defn draw-faces [verts faces colors]
  (doseq [i (range (count faces))]
      (let [vert-idx (faces i)
            v0 (verts (vert-idx 0))
            v1 (verts (vert-idx 1))
            v2 (verts (vert-idx 2))
            v3 (verts (vert-idx 3))
            col-idx (mod i (count colors))
            [r g b a] (if (= (count colors) 12) (colors col-idx) colors)]
        
        (if (seq colors)
          (stroke r g b a))
        ;  (fill (/ (col 0) 3.0) (/ (col 1) 3.0) (/ (col 2) 3.0) 255 ))
        (begin-shape :quads)
        (vertex (v0 0) (v0 1) (v0 2))
        (vertex (v1 0) (v1 1) (v1 2))
        (vertex (v2 0) (v2 1) (v2 2))
        (vertex (v3 0) (v3 1) (v3 2))
        (end-shape))))


(defn draw-faces-lite [verts faces col]
  (apply stroke col)
  (doseq [i (range (count faces))]
    (let [vert-idx (faces i)
          v0 (verts (vert-idx 0))
          v1 (verts (vert-idx 1))
          v2 (verts (vert-idx 2))
          v3 (verts (vert-idx 3))]
      (begin-shape :quads)
      (vertex (v0 0) (v0 1) (v0 2))
      (vertex (v1 0) (v1 1) (v1 2))
      (vertex (v2 0) (v2 1) (v2 2))
      (vertex (v3 0) (v3 1) (v3 2))
      (end-shape))))


(defn face-idxs-to-verts [face-idxs]
  (vec (map #(rd-verts %) face-idxs)))


(defn rotate-vec [v]
  (vec (concat (rest v) [(first v)])))


(defn rotations-vec [v]
    (loop [n (count v)
           accum [v]]
      (if (> n 1)
        (recur (dec n) (conj accum (rotate-vec (last accum))))
        (vec accum))))


(defn facelist-contains-rotations? [face-verts]
  (or
    (> (count (filter #(contains? @face-list %)
                      (rotations-vec face-verts)))
       0)
    (> (count (filter #(contains? @face-list %)
                      (rotations-vec (vec (reverse face-verts)))))
       0)   
       ))


(defn remove-from-facelist [face-verts]
    (let [face-rots (concat (rotations-vec face-verts) 
                            (rotations-vec (reverse face-verts)))]
        (doseq [f face-rots]
          (if (contains? @face-list f)
            (swap! face-list disj f)))))
      
  

; we dont need to check every face in the face list here
; only need to check the neighbours.
(defn add-tile-to-facelist [pos]
  (doseq [f rd-faces]
    (let [fv (face-idxs-to-verts f)
          fvw (vec (map #(vec3-add pos (vec3-scale % 0.5)) fv))]
    (if (not (facelist-contains-rotations? fvw))
      (swap! face-list conj fvw)
      (do
        (remove-from-facelist fvw))))))


(defn build-face-list []
  (reset! face-list #{})
  (doseq [tile (keys (@tiler-state :tiles))]
    (add-tile-to-facelist tile)))


(defn draw-face-list []
  (fill 32 32 32 128)
  ;(fill 32 32 32 128)

  ;(no-fill)
  ;(stroke 0 0 0 192)
  (stroke 40 40 40 190)
  (stroke-weight 4)
  ;(no-stroke)
  (doseq [face-verts @face-list]
    (let [v0 (face-verts 0)
          v1 (face-verts 1)
          v2 (face-verts 2)
          v3 (face-verts 3)]
      (stroke 40 40 40 190) 
      (begin-shape :quads)
      ;(texture @rhomb-tex)
      (vertex (v0 0) (v0 1) (v0 2))
      (vertex (v1 0) (v1 1) (v1 2))
      (vertex (v2 0) (v2 1) (v2 2))
      (vertex (v3 0) (v3 1) (v3 2))
      (end-shape)
      (stroke 40 40 40 128)
      ;(line (v0 0) (v0 1) (v0 2) (v2 0) (v2 1) (v2 2))
      )))


(defn draw-face-list-textured []
  ;(tint 192 192 255 255)
  ;(stroke 0 0 0 192)
  ;(stroke-weight 2)
  (no-stroke)
  (doseq [face-verts @face-list]
    (let [v0 (face-verts 0)
          v1 (face-verts 1)
          v2 (face-verts 2)
          v3 (face-verts 3)
          tex-coord-inset (/ 1.0 7.0)]
      (begin-shape :quads)
      (texture @rhomb-tex)
      (vertex (v0 0) (v0 1) (v0 2) tex-coord-inset 0.5)
      (vertex (v1 0) (v1 1) (v1 2) 0.5 0.0)
      (vertex (v2 0) (v2 1) (v2 2) (- 1.0 tex-coord-inset) 0.5)
      (vertex (v3 0) (v3 1) (v3 2) 0.5 1.0)

      (end-shape)
      ;(line (v0 0) (v0 1) (v0 2) (v2 0) (v2 1) (v2 2))
      )))

; _______________________________________________________________________


(defn draw-selected-tile [pos]
  (let [code ((@tiler-state :tiles) pos)
        col (get-tile-color code)]
    (no-fill)
    (stroke (col 0) (col 1) (col 2) 16)
    (stroke-weight 1)
    (with-translation pos
      (scale 0.5)
      (draw-faces rd-verts rd-faces [(col 0) (col 1) (col 2) 128]))))
   

(defn draw-neighbours [pos]
  (no-fill)
  (stroke 64 64 64 64)
  (stroke-weight 1)
  (let [ipos (vec (map int pos))]
    (doseq [n (get-neighbours ipos)]
      (with-translation n
        (scale 0.5)
        (draw-faces rd-verts rd-faces nil)
        ))))


(defn draw-face-boundaries-basic [pos]
  (when (contains? (@tiler-state :tiles) pos)
    (stroke-weight 4)
    (stroke 0 0 0 255)
    (fill 255 255 255 255)
    (with-translation pos
      (scale 0.5)
      (doseq [i (range 12)]
        (let [[dx dy dz] (co-verts i)
              [dxn dyn dzn] (vec3-normalize [dx dy dz])
              az (Math/atan2 dyn dxn)
              el (- (Math/asin dzn))]
          (with-translation (co-verts i)
            (rotate az 0 0 1)
            (rotate el 0 1 0)
            (box 0.2)))))))


(defn draw-face-boundaries [pos ^String code boundary-mode]
  (when (and (not (nil? code))
             (= 12 (count code))
             (or (contains? (@tiler-state :tiles) pos)
                 (and (= boundary-mode :all)
                      (= (count code) 12))))
    (let [[r g b] (get-tile-color code)]
      (with-translation pos
        (scale 0.5)
        ;(stroke-weight 1)
        (no-stroke)
        ;(stroke r g b 255)
        (assert (and (not (nil? code)) (= 12 (count code))))

        (doseq [^long i (range 12)]
          (when (cond
                  (= boundary-mode :only-empty)
                    (and (is-empty? (@tiler-state :tiles) (get-neighbour-pos pos i))
                         (not= (.charAt code i) \-)
                         (not= (.charAt code i) \0))
                  (= boundary-mode :all)
                    (and (not= (.charAt code i) \-)
                         (not= (.charAt code i) \0))
                  (= boundary-mode :type-change)
                    (and (not= (.charAt code i) \-)
                         (not= (.charAt code i) \0)
                         (not= [r g b] (get-tile-color ((@tiler-state :tiles) (get-neighbour-pos pos i)))))
                  :else
                    false)
            (let [d (.charAt code i)
                  dir (co-verts i)
                  [dx dy dz] (vec3-normalize dir)
                  az (Math/atan2 dy dx)
                  el (- (Math/asin dz))
                  thickness (* 1.3 (bezier-box-thicknesses (.charAt code i)))
                  alpha 255]
              (if (face-digit-like-compatible? d)
                (do (fill 160 160 220 alpha))
                (do
                  (if (>= (int d) 97)
                    (fill 255 255 255 alpha)
                    (fill 0 0 0 alpha))))
              (with-translation (vec3-scale (co-verts i) 0.959)
                (rotate az 0 0 1)
                (rotate el 0 1 0)
                (box 0.125 thickness thickness)))))))))



(defn draw-empty [_tiles]
  (fill 0 255 0 16)
  (stroke 0 255 0 32)
  (doseq [tile (get-empty-positions _tiles (get-in @tiler-state [:params :max-radius]))]
    (let [pos tile]
      (with-translation pos 
        (scale 0.5)
        (box 0.2 0.2 0.2)
        ;(draw-faces rd-verts rd-faces [0 255 0 8])
        ;(draw-faces rd-verts rd-faces rd-face-colors)
        ))))


(defn draw-assemblage-center []
  (let [c @assemblage-center]
    (stroke 255 255 255 192)
    (stroke-weight 4)
    ;(fill 255 255 0 32)
    (no-fill)
    ;(with-translation (find-assemblage-center @tiles)
    (with-translation c
      (scale 0.5)
      (box 1 1 1)
      ;(draw-faces rd-verts rd-faces nil)
      )))

; _______________________________________________________________________




;(defn draw-curve-tangents [f1-idx f2-idx steps]
;  (when (not= f1-idx f2-idx)
;    (let [step (/ 1.0 steps)]
;      (doseq [s (range steps)]
;        (let [t (* s step)
;              b (bbox/get-bezier-point-3d f1-idx f2-idx t)
;              bt (bbox/get-bezier-tangent-3d f1-idx f2-idx t)
;              bts (vec3-add b (vec3-scale bt 0.25))]
;          (line (b 0) (b 1) (b 2) (bts 0) (bts 1) (bts 2)))))))


;(defn draw-curve-solid [f1-idx f2-idx steps]
;  (when (not= f1-idx f2-idx)
;    (doseq [i (range 1 (inc steps))]
;      (let [t (* i (/ 1 steps))
;            bp (get-bezier-point-3d f1-idx f2-idx t)
;            prev-bp (get-bezier-point-3d f1-idx f2-idx (- t (/ 1 steps)))
;            pos (vec3-scale (vec3-add bp prev-bp) 0.5)
;            [dx dy dz] (vec3-normalize (vec3-sub bp prev-bp))
;            az (Math/atan2 dy dx)
;            el (- (Math/asin dz))]
;        (with-translation pos ; bp
;          (scale 0.5)
;          (rotate az 0 0 1)
;          (rotate el 0 1 0)
;          (box 0.25 0.5 0.5))))))
;

(defn draw-curve [f1-idx f2-idx]
  (when (not= f1-idx f2-idx)
    (let [[p1 p2 p3 p4] (bbox/get-bezier-controls f1-idx f2-idx)]
      (bezier (p1 0) (p1 1) (p1 2)
              (p2 0) (p2 1) (p2 2)
              (p3 0) (p3 1) (p3 2)
              (p4 0) (p4 1) (p4 2)))))


(defn draw-curve-with-controls [[p1 p2 p3 p4]]
  (bezier (p1 0) (p1 1) (p1 2)
          (p2 0) (p2 1) (p2 2)
          (p3 0) (p3 1) (p3 2)
          (p4 0) (p4 1) (p4 2)))


(defn make-curve-endpoints-orig [connected-idxs]
  (let [num-points (count connected-idxs)]
    (map #(vector %1 (nth connected-idxs (mod (+ %2 1) num-points)))
         connected-idxs (range num-points))))


;(defn make-curve-endpoints [connected-idxs]
;  (if (> (count connected-idxs) 1)
;    (map vec (vec (combinations connected-idxs 2)))
;    [[(nth connected-idxs 0) (nth connected-idxs 0)]]))

(defn make-curve-endpoints [connected-idxs]
  (if (= (count connected-idxs) 1)
    (list [(first connected-idxs) (first connected-idxs)])
    (map vec (vec (combinations connected-idxs 2)))))


(defn draw-bezier-box-slow [f1-idx f2-idx f1-scale f2-scale steps]
  (let [[f1-off f2-off] (bbox/get-bezier-anchor-offsets f1-idx f2-idx)
        f1-offsets (map #(vec3-scale % f1-scale) f1-off)
        f2-offsets (map #(vec3-scale % f2-scale) f2-off)
        controls (vec (map #(bbox/get-bezier-controls-with-offset f1-idx f2-idx %1 %2)
                       f1-offsets f2-offsets))]
    (doseq [c1-idx (range (count controls))]
      (let [c2-idx (mod (inc c1-idx) 4)
            c1 (controls c1-idx)
            c2 (controls c2-idx)]
        (begin-shape :triangle-strip)
        (doseq [i (range 0 (inc steps))]
          (let [t (* i (/ 1 steps))
                p1 (bbox/get-bezier-point c1 t)
                p2 (bbox/get-bezier-point c2 t)
                center (bbox/get-bezier-point-3d f1-idx f2-idx t)
                ;n1 (vec3-normalize (vec3-sub center p1))
                ;n2 (vec3-normalize (vec3-sub center p2))
                ]
            ;(apply normal n1)
            (apply vertex p1)
            ;(apply normal n2)
            (apply vertex p2)))
        (end-shape)
        ))))


(defn draw-bezier-box-lines [f1-idx f2-idx f1-scale f2-scale steps]
  (let [[f1-off f2-off] (bbox/get-bezier-anchor-offsets f1-idx f2-idx)
         f1-offsets (map #(vec3-scale % f1-scale) f1-off)
         f2-offsets (map #(vec3-scale % f2-scale) f2-off)
         controls (vec (map #(bbox/get-bezier-controls-with-offset f1-idx f2-idx %1 %2)
                            f1-offsets f2-offsets))]
    (doseq [c controls]
      (draw-curve-with-controls c))))
        


(defn draw-gliders [frame]
  (push-style)
  (no-stroke)
  ;(stroke-weight 1)
  ;(stroke 255 255 192 192)
  (doseq [glider @gliders]
    (let [pos (get-glider-pos (glider :id))
          pos2 (get-glider-nextpos (glider :id))
          [dx dy dz] (vec3-normalize (vec3-sub pos pos2))
          az (Math/atan2 dy dx)
          el (- (Math/asin dz))
          col (glider :color)
          tile (glider :current-tile)
          ]
      (if (contains? (@tiler-state :tiles) tile)
        (with-translation pos
          (fill (col 0) (col 1) (col 2) 255)
            (scale 0.005)
            (rotate az 0 0 1)
            (rotate el 0 1 0)
            ;(box 4 1 1)
            ;(box 1 3 1)
            (draw-obj glider-model [128 128 255 255])
            ;(draw-obj (glider-model :vertex) (glider-model :face) [128 128 255 255])
                          ))))
  (pop-style)
  )


(defn draw-facecode-lines [code]
  (let [endpoint-pairs (make-curve-endpoints (get-connected-idxs code))
        num-connected (get-num-connected code)
        col (get-tile-color code); (rd-face-colors (mod num-connected 12))
;        fill-col (rd-face-colors 
;                   (connecting-faces (mod num-connected 12)))
        ]
    (push-style)

    (if (= code nil)
      (do 
        (fill 255 32 32 128)
        ;(no-fill)
        (stroke 255 0 0 192)
        (push-matrix)
        (scale 0.05)
        (box 1 1 1)
        ;(draw-faces rd-verts rd-faces nil)
        (pop-matrix)
        (no-fill)))

    (stroke-weight 4) 
    (stroke (col 0) (col 1) (col 2) 192) 
    
    (if (= num-connected 1)
      (let [p (co-verts (first (get-connected-idxs code)))]
        (line 0 0 0 (p 0) (p 1) (p 2))
        (fill 255 128 128 128)
        ;(box 0.05125 0.05125 0.05125)
        (box 0.25 0.25 0.25)
        (no-fill))
        )

    (stroke-weight 1)
    (stroke (col 0) (col 1) (col 2) 192)
    (doseq [endpoints endpoint-pairs]
      (draw-curve (endpoints 0) (endpoints 1)))
    
    ;draw tangent vectors
    ;(stroke-weight 1)
    ;(stroke 255 120 120 128)
    ;(doseq [endpoints endpoint-pairs]
    ;  (draw-curve-tangents (endpoints 0) (endpoints 1) 3))

    (pop-style)
      ))


(defn draw-facecode-bezier-box-lines [code col steps]
  (when (contains? #{3 4} (count col)) (apply stroke col))
  (stroke-weight @bezier-box-line-weight)
  (no-fill)
  (doseq [line-verts (bbox/get-bezier-box-lines code steps)]
    (doseq [vert line-verts]
      (begin-shape)
      (doseq [[vx vy vz] vert]
        (vertex vx vy vz))
      (end-shape))))


(defn draw-facecode-bezier-boxes [code col steps]
  (when (contains? #{3 4} (count col)) (apply fill col))
  ;(stroke 0 0 0 192)
  (no-stroke)
  ;(stroke-weight 2)
  (doseq [bbox (bbox/get-bezier-box-triangles code steps)]
    (doseq [strip bbox]
      (begin-shape :triangle-strip)
      (doseq [[vx vy vz] strip]
        (vertex vx vy vz))
      (end-shape))))


(defn draw-facecode-bezier-boxes-n [code col steps]
  (when (contains? #{3 4} (count col))
    (apply fill col)
    (apply stroke col)
    (stroke-weight 1)
    )
  ;(stroke 0 0 0 192)
  (no-stroke)
  (no-smooth)
  (doseq [bbox (bbox/get-bezier-box-triangles code steps)]
    (doseq [strip bbox]
      (let [verts (vec strip)
            normals (make-strip-vertex-normals verts)]
        (begin-shape :triangle-strip)
        (doseq [i (range (count verts))]
          (let [[vx vy vz] (verts i)
                [nx ny nz] (normals i)]
            (normal nx ny nz)
            (vertex vx vy vz)))
        (end-shape)))))


(defn draw-facecode-bezier-boxes-n2 [code col steps]
  (when (contains? #{3 4} (count col)) (apply fill col))
  ;(stroke 0 0 0 192)
  (no-stroke)
  (doseq [bbox (vec (bbox/get-bezier-box-triangles code steps))]
    (let [normals1 (vec (bbox/get-bezier-box-normals code steps))]
      (doseq [b (range (count bbox))]
         (let [strip (bbox b)
               verts (vec strip)
               normals (vec (normals1 b))]
          (begin-shape :triangle-strip)
          (doseq [i (range (count verts))]
            (let [[vx vy vz] (verts i)
                  [nx ny nz] (normals i)]
              (normal nx ny nz)
              (vertex vx vy vz)))
          (end-shape))))))


(defn draw-tiling [with-boundaries? with-lines? with-bb-faces? with-bb-lines? boundary-mode]
  (doseq [tile (keys (@tiler-state :tiles))]
    (let [pos tile
          code ((@tiler-state :tiles) pos)
          ;col [255 255 255 255]
          col (conj (get-tile-color code) 255)
          ;line-col [(col 0) (col 1) (col 2) 255]
          line-col col
          ;line-col  [0 0 0 192] ;[192 192 255 192]
          bezier-steps @bezier-box-resolution]
      (with-translation pos 
        (scale 0.5)
        ;(stroke-weight 8)
        ;(stroke 0 0 0 64)
        (no-stroke)
        (when with-lines?
          (no-fill)
          (draw-facecode-lines code))
        (when with-bb-faces?
          (if @bezier-box-smooth-shading?
            (draw-facecode-bezier-boxes-n ((@tiler-state :tiles) pos) col bezier-steps)
            (draw-facecode-bezier-boxes ((@tiler-state :tiles) pos) col bezier-steps)))
        (when with-bb-lines?
          (draw-facecode-bezier-box-lines ((@tiler-state :tiles) pos) line-col bezier-steps))
        )
      (when with-boundaries?
        (draw-face-boundaries pos code boundary-mode))
      
      )))

