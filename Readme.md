### Acne effect on shadows
to determine if a point is in shadow, we calculate if the ray starting from the point itself intercepts any shape in the world when tracing a ray towards the light.

But sometimes the intersect gives a result for t > 0 which is exactly the point the ray started from, falsely interpreting it as a shadow point.

To resolve this we make the point start a bit above the starting point (Pt + epsilon * normV)


### Note about presentation
Try with LaTeX Beamer theme.
To sync notes and pdf I'm using pympress (yes, I'm putting my reputation in the hands of Python, a language I don't master). I followed https://www.scivision.dev/beamer-latex-dual-display-pdf-notes/. I ran

```
brew install gtk+3 poppler gobject-introspection pygobject3
python -m pip install --user pympress
```

