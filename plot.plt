# set term postscript eps enhanced "Helvetica" 20 
set terminal png
# set size 0.7,0.7
set output "figure1.png"

# Axes
set xrange [0:1500]
set xlabel "Buffer size"
# set xtics 0,300,1500

set yrange [0:10]
set ylabel "Timeout (ms)"

set zrange [0:50]
set zlabel "Débit (Mb/s)"

# Options
set key box left height 0.5
set grid xtics ytics
set style line 1 lc 3  linewidth 2

# plot
plot "trace.log" using 1:2:5 title "Performances" with points pt 5 ps 0.1 lc rgb "#AAAAAA"
