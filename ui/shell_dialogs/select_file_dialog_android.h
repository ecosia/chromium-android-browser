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

#ifndef UI_SHELL_DIALOGS_SELECT_FILE_DIALOG_ANDROID_H_
#define UI_SHELL_DIALOGS_SELECT_FILE_DIALOG_ANDROID_H_

#include <jni.h>

#include "base/android/scoped_java_ref.h"
#include "base/files/file_path.h"
#include "ui/shell_dialogs/select_file_dialog.h"

namespace ui {

class SelectFileDialogImpl : public SelectFileDialog {
 public:
  static SelectFileDialogImpl* Create(Listener* listener,
                                      std::unique_ptr<SelectFilePolicy> policy);

  SelectFileDialogImpl(const SelectFileDialogImpl&) = delete;
  SelectFileDialogImpl& operator=(const SelectFileDialogImpl&) = delete;

  void OnFileSelected(JNIEnv* env,
                      const base::android::JavaParamRef<jobject>& java_object,
                      const base::android::JavaParamRef<jstring>& filepath,
                      const base::android::JavaParamRef<jstring>& display_name);

  void OnMultipleFilesSelected(
      JNIEnv* env,
      const base::android::JavaParamRef<jobject>& java_object,
      const base::android::JavaParamRef<jobjectArray>& filepaths,
      const base::android::JavaParamRef<jobjectArray>& display_names);

  void OnFileNotSelected(
      JNIEnv* env,
      const base::android::JavaParamRef<jobject>& java_object);

  void OnContactsSelected(
      JNIEnv* env,
      const base::android::JavaParamRef<jobject>& java_object,
      const base::android::JavaParamRef<jstring>& contacts);

  // From SelectFileDialog
  bool IsRunning(gfx::NativeWindow) const override;
  void ListenerDestroyed() override;

  // Called when it is time to display the file picker.
  // params is expected to be a vector<string16> with accept_types first and
  // the capture value as the last element of the vector.
  void SelectFileImpl(SelectFileDialog::Type type,
                      const std::u16string& title,
                      const base::FilePath& default_path,
                      const SelectFileDialog::FileTypeInfo* file_types,
                      int file_type_index,
                      const std::string& default_extension,
                      gfx::NativeWindow owning_window,
                      void* params,
                      const GURL* caller) override;

 protected:
  ~SelectFileDialogImpl() override;

 private:
  SelectFileDialogImpl(Listener* listener,
                       std::unique_ptr<SelectFilePolicy> policy);

  bool HasMultipleFileTypeChoicesImpl() override;

  base::android::ScopedJavaGlobalRef<jobject> java_object_;
};

}  // namespace ui

#endif  // UI_SHELL_DIALOGS_SELECT_FILE_DIALOG_ANDROID_H_
