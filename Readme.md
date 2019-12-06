# A Ray tracer built using ZIO to learn environmental effects

For a detailed explanation, see [here](https://github.com/pierangeloc/ray-tracer-zio/blob/master/post/ray-tracer.md)

### Run ppm programs
Under `ray-tracer/io.tuliplogic.raytracer.ops.programs` you'll find a few programs that write `.ppm` files in a directory. The world for these images is built programmatically

### Run http service

It is possible also to have images served through http.


1. Run the http server (port 8080)

```
> simple-http4s/runMain io.tuliplogic.raytracer.http.model.Main`
```

2. Describe a world as a json, e.g. the same world that is described programmatically in `io.tuliplogic.raytracer.ops.programs.Chapter10World2` corresponds to the file [`world10w2.json`](simple-http4s/worlds/world10w2.json)

3. Send a POST to `localhost:8080/ray-tracer/draw`
 
```
curl -X POST \
  http://localhost:8080/ray-tracer/draw \
  -d '{<content of the json file defined at point 2>}'
```

This will provide an ID

```json
{
    "value": 5929599111289830703
}
```

4. Get the world rendering from the Id

```
curl -X GET \
  http://localhost:8080/ray-tracer/draw/5929599111289830703 
```

If the image is ready you will get the png, otherwise you will get a json message that tells when the image computation was submitted


You can also retrieve the list of submitted world computations (regardless if computed or in progress)

```
curl -X GET \
  http://localhost:8080/ray-tracer/draw
```