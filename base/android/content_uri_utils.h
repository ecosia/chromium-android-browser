/*
 * Copyright 2013 The Chromium Authors
 * Copyright (C) 2023 Ecosia Android App source (for GPL 3.0)
 *
 * Licensed under the GNU General Public License, Version 3.0 and BSD-style license (found in LICENSE file);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * License: GPL-3.0-only - https://spdx.org/licenses/GPL-3.0-only.html
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#ifndef BASE_ANDROID_CONTENT_URI_UTILS_H_
#define BASE_ANDROID_CONTENT_URI_UTILS_H_

#include <jni.h>
#include <string>

#include "base/base_export.h"
#include "base/files/file.h"
#include "base/files/file_path.h"

namespace base {

// Opens a content URI for read and returns the file descriptor to the caller.
// Returns -1 if the URI is invalid.
BASE_EXPORT File OpenContentUriForRead(const FilePath& content_uri);

// Ecosia: Bookmark Import / Export
// Opens a content URI for write and returns the file descriptor to the caller.
// Returns -1 if the URI is invalid.
BASE_EXPORT File OpenContentUriForWrite(const FilePath& content_uri);

// Check whether a content URI exists.
BASE_EXPORT bool ContentUriExists(const FilePath& content_uri);

// Gets MIME type from a content URI. Returns an empty string if the URI is
// invalid.
BASE_EXPORT std::string GetContentUriMimeType(const FilePath& content_uri);

// Gets the display name from a content URI. Returns true if the name was found.
BASE_EXPORT bool MaybeGetFileDisplayName(const FilePath& content_uri,
                                         std::u16string* file_display_name);

// Deletes a content URI.
BASE_EXPORT bool DeleteContentUri(const FilePath& content_uri);

// Gets content URI's file path (eg: "content://org.chromium...") from normal
// file path (eg: "/data/user/0/...").
BASE_EXPORT FilePath GetContentUriFromFilePath(const FilePath& file_path);

}  // namespace base

#endif  // BASE_ANDROID_CONTENT_URI_UTILS_H_
