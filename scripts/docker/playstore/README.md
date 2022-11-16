# Create Chromium image for Playstore Deployments

Builds and creates a docker image including all the prebuilds we need for a Playstore deployment.

- arm    Lollipop / 5.0+ (chrome_modern_public_bundle)
- arm64  Nougat   / 7.0+ (monochrome_public_bundle)
- x86    Lollipop / 5.0+ (chrome_modern_public_bundle)
- x64    Lollipop / 5.0+ (chrome_modern_public_bundle)
- **experimental** arm64  Q / 10.0+ (trichrome_chrome_bundle)

## Steps

1. Cd in to this directory
2. Edit `Dockerfile` to have the desired Docker image to build from. It is optimized to build on top of an AppCenter image.

    ```Dockerfile
    FROM ecosiadev/chromium:vanilla-85.0.4183.127-arm64-monochrome
    ```

3. Built the image. Tag it as you like.

    ```sh
    docker built -t ecosiadev/chromium:vanilla-85.0.4183.127-playstore .
    ```

After building the image it will occur in your local docker registy
