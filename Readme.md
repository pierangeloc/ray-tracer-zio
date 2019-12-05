# A Ray tracer built using ZIO to learn environmental effects

For a detailed explanation, see [here](https://github.com/pierangeloc/ray-tracer-zio/blob/master/post/ray-tracer.md)


### Run http service

On sbt
```
> simple-http4s/runMain io.tuliplogic.raytracer.http.model.Main`
```

httpie
```
(http POST localhost:8080/ray-tracer/draw --timeout=60 < world10w2.json)
```