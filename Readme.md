# A Ray tracer built using ZIO to learn environmental effects

For a detailed explanation, see [here](https://github.com/pierangeloc/ray-tracer-zio/blob/master/post/ray-tracer.md)

### Run ppm programs
Under `ray-tracer/io.tuliplogic.raytracer.ops.programs` you'll find a few programs that write `.ppm` files in a directory. The world for these images is built programmatically

### Run http service

It is possible also to have images served through http.


1. Run docker-compose
```
> docker-compose up
```

1. Run the http server (port 8080)

```
> simple-http4s/reStart`
```

2. Open swagger [docs](http://localhost:8090/docs/index.html?url=/docs/docs.yaml)

3. Create a user
4. Update user password
5. Login and get an access token
6. Use the access token to submit a computation to `POST /scene`
7. Check the status of the submitted scenes `GET /scene`
8. Access a single complete scene png with `GET /scene/<sceneId>/png`