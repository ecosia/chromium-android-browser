# Create Chromium image for AppCenter Deployments

Builds and creates a docker image including prebuilt Vanilla Chromium for arm64 Monochrome (7.0+). After building the image it will occur in your local docker registy

## Steps

1. cd in to this directory
2. Edit `Dockerfile` to have the desired Docker Image to build from

    ```Dockerfile
    FROM ecosiadev/chromium:vanilla-85.0.4183.127-synced
    ```

3. Built the image. Tag it as you like.

    ```sh
    docker built -t ecosiadev/chromium:vanilla-85.0.4183.127-arm64-monochrome .
    ```

## NOTE

This step includes running a modified `install-build-deps.sh`. This will install all needed Android dependencies inside the container. This is just a copy from the Vanilla Chromium file with deactivated `snapcraft` package. This is not needed for our process but will fail to install inside Docker.

```bash
#if package_exists snapcraft; then
#    dev_list="${dev_list} snapcraft"
#fi
```
