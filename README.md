![MIT/Apache 2.0 Licensed](https://img.shields.io/badge/license-MIT-blue)
[![Java](https://github.com/shampoofactory/async-download/actions/workflows/maven.yml/badge.svg)](https://github.com/shampoofactory/async-download/actions)

# async-download
Java Apache HttpAsyncClient toy downloader

An exercise in creating a concurrent download tool based on Apache's HttpAsyncClient.


## Build

This project uses Maven (version 3) as a build tool.

```
$ mvn package
$ cd target
$ java -jar AsyncDownload-1.0-SNAPSHOT-shaded.jar https://www.httpbin.org filename
```

## License

This project is licensed under the terms of the MIT license.


## References

https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
