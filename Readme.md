### Acne effect on shadows
to determine if a point is in shadow, we calculate if the ray starting from the point itself intercepts any shape in the world when tracing a ray towards the light.

But sometimes the intersect gives a result for t > 0 which is exactly the point the ray started from, falsely interpreting it as a shadow point.

To resolve this we make the point start a bit above the starting point (Pt + epsilon * normV)