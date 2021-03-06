(ns rhombrick.editor
  (:use [quil.core]
        [rhombrick.vector]        
        [rhombrick.tilecode]
        [rhombrick.tiling]
        [rhombrick.tiling-render]
        [rhombrick.bezierbox :as bbox]
        [rhombrick.button]
        [rhombrick.staticgeometry]
        [rhombrick.tileset-library]
        [rhombrick.console :as console]))

;(def current-tileset-colors {})

(def button-color {
                   :fill [8 0 128 240]
                   :stroke [0 0 0 0]
                   :fill-hot [128 128 255 192]
                   :stroke-hot [0 0 192 192]
                   :fill-active [255 0 0 192]
                   :stroke-active [255 255 255 192]
                   })

;(def default-tileset ["A-A---------" "0-a---0-----" "4-d---D-3---" "3-------D---" "D-d---0-0---" "D-0---6-a---" "A-----6-----" "a-----d-0---" "A-----a-----" "6-----a-----"])
(def default-tileset (make-random-tileset 1 [])) 


(def default-editor-state {:level 0
                           :selected [0 0 0]
                           :selected-tilecode-digit []
                           :tileset (atom default-tileset)
                           :tileset-topology (@current-topology :id) 
                         })

(def editor-state (atom default-editor-state))
(def library-tilesets (atom []))
(def library-tileset-index (atom 0))

(def next-digit {\- \0
                 \0 \1
                 \1 \2
                 \2 \3
                 \3 \4
                 \4 \5
                 \5 \6
                 \6 \7
                 \7 \a
                 \a \A
                 \A \b
                 \b \B
                 \B \c
                 \c \C
                 \C \d
                 \d \D
                 \D \-
                 })

(def symmetry-display-index (atom 0))

(defn get-selected [level]
  ((@editor-state :selected) level))


(defn set-selected [i level]
  (let [selected (get-selected level) ]
    (swap! editor-state assoc :selected
                              (assoc (@editor-state :selected) level i))))


(defn get-tileset []
  @(@editor-state :tileset))

(defn get-tileset-topo-id []
  (@editor-state :tileset-topology))

(defn get-tileset-as-set []
  (set (get-tileset)))

(defn get-tileset-expanded []
  (expand-tiles-preserving-symmetry (get-tileset)))
  

(defn add-to-tileset [tile]
  (let [idx (count (get-tileset))]
    (swap! (@editor-state :tileset) assoc idx tile)))


(defn set-tileset [tileset]
  ;(console/writeline (str "tileset:" tileset))
  (println "set-tileset: " tileset)
;  (let [;tileset-set (distinct tileset)
;        num-uniq (count tileset)]
  (reset! (@editor-state :tileset) [])
  (reset! current-tileset-colors {})
  (bbox/bezier-box-cache-reset)
  
  (let [topo (topologies (@editor-state :tileset-topology))
        col-offset (mod (tileset-to-number tileset) (topo :num-faces))]
    (doseq [i (range (count tileset))]
      (let [code (tileset i)
            col-idx (mod (+ i col-offset) (topo :num-faces))
            col (phi-palette-color col-idx 0) ]
        ;(when-not (set-contains-rotations? (set tileset) code)
        (add-to-tileset code)
        (doseq [rc (get-code-symmetries code)]
          (swap! current-tileset-colors assoc rc col)))))
  (if (> ((@editor-state :selected) 1) (dec (count (get-tileset))))
    (set-selected (dec (count (get-tileset))) 1))
  ;(reset! adhd 2.0)
  ;(reset! autism 1.0)
  ;(reset! adapt-last-tilecount 0)
  ;(init-dead-loci)
  ; (soft-init-tiler)
  )


(defn set-tileset-topo-id [topo-id]
  (reset! editor-state (assoc @editor-state :tileset-topology topo-id))  )

(defn valid-level? [l]
  (and (>= l 0) (< l 3)))


(defn get-level []
  (@editor-state :level))


(defn set-level [l]
  (if (valid-level? l)
    (swap! editor-state assoc :level l)))


(defn get-max-selected-idx []
  (let [level (get-level)]
    (cond 
      (= level 1) 
        (count (get-tileset))
      (= level 2)
        (@current-topology :num-faces))))


(defn index-exclude [r ex] 
   "Take all indices except ex" 
    (filter #(not (ex %)) (range r))) 


(defn dissoc-idx [v & ds]
   (map v (index-exclude (count v) (into #{} ds))))


(defn replace-facecode-digit [code idx d]
  (apply str (map-indexed #(if (= %1 idx) d %2) code)))


(defn set-current-tileset-digit [tile-idx idx d]
  (when-let [tileset (get-tileset)]
    (when (< tile-idx (count tileset))
      (let [code (tileset tile-idx)
            new-code (replace-facecode-digit code idx d)
            new-tileset (assoc tileset tile-idx new-code)]
        (set-tileset new-tileset)))))


(defn key-edit-tilecode []
  (when-let[tileset (get-tileset)] ; (> (count (get-tileset)) 0)
    (let [selected-tile-idx (get-selected 1)
          selected-code (tileset selected-tile-idx)
          selected-digit-idx (get-selected 2)
          selected-digit (nth selected-code selected-digit-idx)
          new-digit (next-digit selected-digit)]
      (println "tileset:" tileset
               "selected-tile-idx:" selected-tile-idx
               "selected-code:" selected-code
               "selected-digit-idx" selected-digit-idx
               "selected-digit" selected-digit)
      (set-current-tileset-digit selected-tile-idx
                                 selected-digit-idx
                                 new-digit)
      (start-tiler (get-tileset-as-set) false))))


(defn level-up []
  (if (and ( < (get-level) 2)
           (> (count (get-tileset)) 0))
    (set-level (inc (@editor-state :level)))
    (key-edit-tilecode)))

(defn level-down []
  (set-level (dec (@editor-state :level))))

(defn move-left []
  (let [level (get-level)]
    (if (> level 0)
      (set-selected (mod (dec (get-selected level))
                         (get-max-selected-idx))
                    level))))
(defn move-right []
  (let [level (get-level)]
    (if (> level 0)
      (set-selected (mod (inc (get-selected level))
                         (get-max-selected-idx))
                    level))))


(defn load-library-tileset [idx]
  (when (< idx (count @library-tilesets))
    (set-tileset-topo-id ((@library-tilesets idx) 0))
    (reset! current-topology (topologies (get-tileset-topo-id)))
    (set-tileset ((@library-tilesets idx) 1))
    ))


(defn load-next-library-tileset []
  (let [new-idx (mod (inc @library-tileset-index)
                     (count @library-tilesets))]
    (reset! library-tileset-index new-idx)
    (load-library-tileset new-idx)))


(defn load-prev-library-tileset []
  (let [new-idx (mod (dec @library-tileset-index)
                     (count @library-tilesets))]
    (reset! library-tileset-index new-idx)
    (load-library-tileset new-idx)))


(defn save-current-tileset-to-library []
  (let [ts (get-tileset)
        topo-id (get-tileset-topo-id)]
    (save-tileset-to-library [topo-id ts])
    (swap! library-tilesets conj [topo-id ts])))


(defn remove-from-current-tileset [code]
  (let [new-tileset (vec (filter #(not= code %) (get-tileset)))]
    (set-tileset new-tileset)))

;(defn remove-from-current-tileset [code]
;  (swap! current-tileset-colors dissoc code)
;  (swap! current-tileset disj code))


(defn init-editor []
  (reset! editor-state default-editor-state)
  (reset! library-tilesets (load-tileset-library)))


(defn draw-face-idx-numbers [pos use-face-color?]
  (no-lights)
  (doseq [i (range (@current-topology :num-faces))]
    (let [[r g b] (phi-palette-color i 0)]
      (with-translation pos
        ;(scale 0.5)
        (stroke-weight 1)
        (stroke 255 255 255 128)
        (fill 255 255 255 255)                
        (let [dir ((@current-topology :face-centers) i)
             [dx dy dz] (vec3-normalize dir)
             az (Math/atan2 dy dx)
             el (- (Math/asin dz))
             tw (text-width (str i))]
          (if use-face-color?
            (fill r g b 192)
            (fill 255 255 255 192))
          (with-translation (vec3-scale ((@current-topology :face-centers) i) 1.01) ;0.975)
            (rotate az 0 0 1)
            (rotate el 0 1 0)
            (scale 0.025)
            (rotate-y (* Math/PI 0.5))
            (if use-face-color?
              (translate -10 0 0)
              (translate 10 0 0))
            (text (str i) (- tw) 0 0)                
                            ))))))


(defn draw-selected [[x y] bscale col]
  (apply stroke col)
  (no-fill)
  (rect x y bscale bscale))


(defn draw-facecode-buttons [[x y] sc code parent-idx]
  (let [num-buttons (@current-topology :num-faces) 
        bspace 1
        bsize (/ (- sc (* bspace (- num-buttons 1)))
                 num-buttons)
        txt-off-x (- (/ bsize 2) 4)
        txt-off-y (+ 5 (/ bsize 2))
        level (get-level)
        selected (get-selected 2)]
    (doseq [i (range num-buttons)]
      (let [bx (+ x (+ (* i bsize) (* i bspace)))
            by y
            tx (+ bx txt-off-x)
            ty (+ by txt-off-y)]
        (if (button bx by bsize bsize button-color (str (.charAt code i)))
          (println "button pressed:" code))
        (if (and (= level 2)
                 (= i selected)
                 (= parent-idx (get-selected 1)))
          (draw-selected [bx by] bsize [255 255 255 255]))
        (fill 255 255 255 255)
        (text (str (.charAt code i)) tx ty)))))


(defn draw-tile-button [[x y] code bscale parent-idx]
  (let [bx (+ x (/ bscale 2))
        by (+ y (/ bscale 2))
        col (conj (get-tile-color code) 240)
        face-col [64 64 64 190]
        ]
    (stroke-weight 1)
    (when (button x y bscale bscale button-color code)
      (do
        (println "button pressed:" code)))
    (with-translation [bx by]
      (scale (/ bscale 3))
      (no-fill)
      (stroke-weight 1)
      ;(draw-faces-lite (@current-topology :verts) (@current-topology :faces) face-col)
      (if (or (= (@current-topology :id) :hexagon)
              (= (@current-topology :id) :square))
      (draw-jigsaw-shape-2d code @current-topology 16)
      (draw-facecode-bezier-boxes code col 8)
      ;(scale 2)
      ;(draw-face-boundaries [0 0 0] code :all)
      ))))


(defn draw-tile-editor [[x y] code bscale parent-idx]
  (let [bx (+ x (/ bscale 2))
        by (+ y (/ bscale 2))
        col (get-tile-color code)
        ;ang ((symmetries-flattened @symmetry-display-index) 0)
        ;rads (* (/ ang 180) Math/PI)
        ;[dx dy dz] ((symmetries-flattened @symmetry-display-index) 1)
        ]
    (stroke-weight 1)
    (when (button x y bscale bscale button-color code)
      (do
        (println "button pressed:" code)))
    (draw-facecode-buttons [x (+ y bscale 5)] bscale code parent-idx)
    (with-translation [bx by]
        (text (apply str "symmetries:" (interpose ", " (distinct (get-tilecode-angle-ids code))))
              (- bx bscale) (- by bscale))
        (scale (/ bscale 5))
        (rotate-y (* (frame-count) 0.0051471))
        (no-fill)
        (stroke-weight 1)
        (hint :enable-depth-test)
        (draw-faces (@current-topology :verts) (@current-topology :faces) [128 128 128 192])
        (no-fill)
        (draw-facecode-bezier-boxes code col 8)
        (draw-facecode-bezier-box-lines code col 8)
        ;(scale 2)
        (draw-face-boundaries [0 0 0] code :all)
        (no-fill)
        (draw-face-idx-numbers [0 0 0] false)
                      )))


(defn draw-tileset-editor [[x y] tileset bscale]
  (ui-prepare)
  (let [level (get-level)
        selected (get-selected 1)
        preview-pos [x (+ y bscale 10)]
        preview-scale 640
        rotations-pos [(+ preview-scale (preview-pos 0)) (preview-pos 1)  ]
        rotations-scale 180]
    (doseq [i (range (count tileset))]
      (let [code (tileset i)
            tx (+ x (* i (+ bscale (/ bscale 8))))
            ty y]
        (draw-tile-button [tx ty] code bscale i)
        (when (and (> level 0) (= i selected))
          (draw-selected [tx ty] bscale [255 255 255 255]))
        (when (and (> level 0) (= i selected))
          ;(draw-tile-editor preview-pos code preview-scale i)
          ;(draw-selected preview-pos preview-scale [255 255 255 255])
          ;(draw-rotational-symmetries rotations-pos code rotations-scale)
          ;(draw-rotations rotations-pos code rotations-scale)))))
          ))))
  (ui-finish))

