# Create synced Chromium image

Creates a synced docker images from a Chromium tag. The image will then occur in your local docker registy

```sh
~ docker images
ecosiadev/chromium  vanilla-85.0.4183.127-synced  ba4a19756815  5 days ago  17GB
```

## Requirements

- Ubuntu 18.04 or greater
- 20 GB of storage
- docker, git, python

## Steps

1. Copy contents of this folder to any place that has 20GB of storage left.
2. Edit `build.sh` to have the desired Chromium tag.

    ```sh
    chromium_version=85.0.4183.127
    ```

3. run `build.sh` and grab some beers. It might take at least 30 min.

    ```sh
    ./build.sh
    ```
