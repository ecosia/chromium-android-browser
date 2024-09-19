#!/usr/bin/env python3

# This file is part of eyeo Chromium SDK,
# Copyright (C) 2006-present eyeo GmbH
# eyeo Chromium SDK is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3 as
# published by the Free Software Foundation.
# eyeo Chromium SDK is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# You should have received a copy of the GNU General Public License
# along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.
"""
Script to get pre-signed URL of S3 artifacts

"""

import argparse
import os
import sys
import boto3
import fnmatch

from datetime import timedelta
from urllib.parse import urlparse


def get_matching_s3_objects(bucket, prefix, pattern):
    s3 = boto3.client('s3')
    response = s3.list_objects_v2(Bucket=bucket, Prefix=prefix)
    for item in response.get('Contents', []):
        filename = os.path.basename(item['Key'])
        if fnmatch.fnmatch(filename, pattern):
            return filename  # Return the first matching item

def get_presigned_artifact_url(bucket, s3_key, expiration_in_days):
    s3 = boto3.client('s3', region_name='eu-central-1')
    try:
        # Check if object exists
        s3.head_object(Bucket=bucket, Key=s3_key)
        url = s3.generate_presigned_url('get_object',
                                        Params={
                                            'Bucket': bucket,
                                            'Key': s3_key
                                        },
                                        ExpiresIn=expiration_in_days * 86400)
        return url

    except s3.exceptions.NoSuchKey:
        print(
            f"The object with key {s3_key} does not exist in the bucket {bucket}."
        )
    except Exception as e:
        print(f"An error occurred: {e}")


def main(args):
    url = None
    if args.search_pattern and args.prefix:
        filename = get_matching_s3_objects(args.bucket, args.prefix,
                                           args.search_pattern)
        if filename:
            s3_key = os.path.join(args.prefix, filename)
            url = get_presigned_artifact_url(args.bucket, s3_key,
                                             args.expiration)
        else:
            print(f"No matching object found in {args.bucket}/{args.prefix}")
    elif args.s3_key:
        url = get_presigned_artifact_url(args.bucket, args.s3_key,
                                         args.expiration)
    else:
        print("Please provide s3_key or prefix and search pattern")
    if url:
        print(url)
    else:
        print("Failed to generate a presigned URL.")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)

    parser.add_argument(
        "bucket",
        type=str,
        help="The name of s3 bucket",
        default="chromium-sdk",
    )

    parser.add_argument(
        "--s3_key",
        type=str,
        help="Path to s3 object (Prefix + filename)",
    )

    parser.add_argument(
        "--prefix",
        type=str,
        help="Prefix or path where the file is located in the bucket",
    )

    parser.add_argument(
        "--search-pattern",
        type=str,
        help="Search pattern for the filename",
    )

    parser.add_argument(
        "--expiration",
        type=int,
        help="Expiration time in days",
        default=1,
    )

    args = parser.parse_args()
    main(args)
