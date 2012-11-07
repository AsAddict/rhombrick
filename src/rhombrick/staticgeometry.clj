(ns rhombrick.staticgeometry)
; _______________________________________________________________________
;
; Rhombic dodecahedron
; Faces     12
; Edges     24
; Vertices  14

(def rd-verts [
  [ 1 -1 -1]
  [ 1 -1  1]
  [-1 -1  1]
  [-1 -1 -1]
  [ 1  1 -1]
  [ 1  1  1]
  [-1  1  1]
  [-1  1 -1]
  [ 0  0  2]
  [-2  0  0]
  [ 2  0  0]
  [ 0 -2  0]
  [ 0  0 -2]
  [ 0  2  0]
])

(def rd-faces [
               [0 10 1 11]
               [5 8 1 10]
               [4 13 5 10]
               [6 8 5 13]
               [7 13 4 12]
               [0 12 4 10]
               [0 11 3 12]
               [1 8 2 11]
               [6 9 2 8]
               [7 9 6 13]
               [7 12 3 9]
               [3 11 2 9]
])

;(def rd-face-colors [
;                  [255   0   0] ;  0 red
;                  [255 128   0] ;  1 orange
;                  [255 255   0] ;  2 yellow
;                  [  0 255   0] ;  3 green
;                  [  0   0 255] ;  4 blue
;                  [128   0 255] ;  5 purple
;                  [  0 128   0] ;  9 dark green
;                  [  0   0 128] ; 10 dark blue
;                  [ 64   0 128] ; 11 dark purple
;                  [128   0   0] ;  6 dark red
;                  [128  64   0] ;  7 dark orange
;                  [128 128   0] ;  8 dark yellow
;                  ])

(def rd-face-colors [
                  [255   0   0] ;  0 red
                  [255 128   0] ;  1 orange
                  [255 255   0] ;  2 yellow
                  [128 255   0] ;  3 light green 
                  [  0 255   0] ;  4 green
                  [  0 255 128] ;  5 purple
                  [  0 255 255] ;  9 cyan
                  [  0 128 255] ; 10 green-blue
                  [  0   0 255] ; 11 blue
                  [128   0 255] ;  6 
                  [255   0 255] ;  7 magenta
                  [255   0 128] ;  8 pink
                  ])

(def rd-neighbour-offsets-orig [
                       [ 1 -1  0]
                       [ 1  0  1]
                       [ 1  1  0]
                       [ 0  1  1]
                       [ 0  1 -1]
                       [ 1  0 -1]
                       [ 0 -1 -1]
                       [ 0 -1  1]
                       [-1  0  1]
                       [-1  1  0]
                       [-1  0 -1]
                       [-1 -1  0]])

(def rd-neighbour-offsets [
                       [ 1 -1  0]
                       [ 1  0  1]
                       [ 1  1  0]
                       [ 0  1  1]
                       [ 0  1 -1]
                       [ 1  0 -1]
                       [-1  1  0]
                       [-1  0 -1]
                       [-1 -1  0]
                       [ 0 -1 -1]
                       [ 0 -1  1]
                       [-1  0  1]
                       ])

;
; Cuboctahedron
; Faces     14
; Edges     24
; Vertices  12
;
; Being the dual of the rhombic dodecahedron, this
; gives us the vertices of the centers of the 
; faces of said polyhedron and are also used to 
; calculate face normals.

(def co-verts-orig [
               [ 1 -1  0] ;  0 red
               [ 1  0  1] ;  1 orange
               [ 1  1  0] ;  2 yellow
               [ 0  1  1] ;  3 green
               [ 0  1 -1] ;  4 blue
               [ 1  0 -1] ;  5 purple
               [ 0 -1 -1] ;  9 dark green
               [ 0 -1  1] ; 10 dark blue
               [-1  0  1] ; 11 dark purple
               [-1  1  0] ;  6 dark red
               [-1  0 -1] ;  7 dark orange
               [-1 -1  0] ;  8 dark yellow
               ])

(def co-verts [
                       [ 1 -1  0]
                       [ 1  0  1]
                       [ 1  1  0]
                       [ 0  1  1]
                       [ 0  1 -1]
                       [ 1  0 -1]
                       
                       [-1  1  0]
                       [-1  0 -1]
                       [-1 -1  0]
                       [ 0 -1 -1]
                       [ 0 -1  1]
                       [-1  0  1]

               ])


; edge-centered cubic lattice logic
; 
; V        V % 2
; ==============
; 0 0 0 ,  0 0 0
; 0 0 1 ,  0 0 1 *
; 0 0 2 ,  0 0 0
; --------------
; 0 1 0 ,  0 1 0 *
; 0 1 1 ,  0 1 1
; 0 1 2 ,  0 1 0 *
; --------------
; 0 2 0 ,  0 0 0
; 0 2 1 ,  0 0 1 *
; 0 2 2 ,  0 0 0
; ==============
; 1 0 0 ,  1 0 0 *
; 1 0 1 ,  1 0 1
; 1 0 2 ,  1 0 0 *
; --------------
; 1 1 0 ,  1 1 0
; 1 1 1 ,  1 1 1 *
; 1 1 2 ,  1 1 0
; --------------
; 1 2 0 ,  1 0 0 *
; 1 2 1 ,  1 0 1
; 1 2 2 ,  1 0 0 *
; ==============
; 2 0 0 ,  0 0 0
; 2 0 1 ,  0 0 1 *
; 2 0 2 ,  0 0 0
; --------------
; 2 1 0 ,  0 1 0 *
; 2 1 1 ,  0 1 1
; 2 1 2 ,  0 1 0 *
; --------------
; 2 2 0 ,  0 0 0
; 2 2 1 ,  0 0 1 *
; 2 2 2 ,  0 0 0

;
;
; ---------------------------------------------------------------------------
; The rhombick dodecahedron has octahedral symmetry. A regular octahedron
; has 24 rotational (or orientation-preserving) symmetries, and a symmetry
; order of 48 including transformations that combine a reflection and a
; rotation.

; We will be using chiral octahedral symmetry (24 symmetries, no reflections):
;
; • identity
; • 6 × rotation by 90°
; • 8 × rotation by 120°
; • 3 × rotation by 180° about a 4-fold axis
; • 6 × rotation by 180° about a 2-fold axis
;
; The axes for six 180° rotations around the 6 2-fold axes can be found by
; looking at the vectors formed by opposing pairs of cuboctahedron vertices.
; We know that there are no opposing vertices in the first 6 of co-verts,
; so :
; => (map vec3-normalize (take 6 co-verts)))
;
;
;
(def symmetries
  {
  ;:identity      -

   90             [[ 0  0  1]
                   [-1  0  0]
                   [ 1  0  0]
                   [ 0 -1  0]
                   [ 0  0 -1]
                   [ 0  1  0]]

   120            [[ 0.5773502691896258 -0.5773502691896258 -0.5773502691896258]
                   [ 0.5773502691896258 -0.5773502691896258  0.5773502691896258]
                   [-0.5773502691896258 -0.5773502691896258  0.5773502691896258]
                   [-0.5773502691896258 -0.5773502691896258 -0.5773502691896258]
                   [ 0.5773502691896258  0.5773502691896258 -0.5773502691896258]
                   [ 0.5773502691896258  0.5773502691896258  0.5773502691896258]
                   [-0.5773502691896258  0.5773502691896258  0.5773502691896258]
                   [-0.5773502691896258  0.5773502691896258 -0.5773502691896258]]

   180            [[0 0 1]
                   [0 1 0]
                   [1 0 0]
                   [0.7071067811865475  -0.7071067811865475   0.0]
                   [0.7071067811865475   0.0                  0.7071067811865475]
                   [0.7071067811865475   0.7071067811865475   0.0]
                   [0.0                  0.7071067811865475   0.7071067811865475]
                   [0.0                  0.7071067811865475  -0.7071067811865475]
                   [0.7071067811865475   0.0                 -0.7071067811865475]]
   }
)
