/*
 * Copyright 2012 The Chromium Authors
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

#ifndef CHROME_BROWSER_IMPORTER_PROFILE_WRITER_H_
#define CHROME_BROWSER_IMPORTER_PROFILE_WRITER_H_

#include <string>
#include <vector>

#include "base/memory/raw_ptr.h"
#include "base/memory/ref_counted.h"
#include "build/build_config.h"
#include "components/bookmarks/browser/bookmark_model.h" // Ecosia: Bookmark Import / Export
#include "components/favicon_base/favicon_usage_data.h"
#include "components/history/core/browser/history_types.h"
#include "components/search_engines/template_url_service.h"
#include "url/gurl.h"

struct ImportedBookmarkEntry;
class Profile;

namespace autofill {
class AutofillEntry;
}

namespace password_manager {
struct PasswordForm;
}  // namespace password_manager

// ProfileWriter encapsulates profile for writing entries into it.
// This object must be invoked on UI thread.
class ProfileWriter : public base::RefCountedThreadSafe<ProfileWriter> {
 public:
  explicit ProfileWriter(Profile* profile);

  ProfileWriter(const ProfileWriter&) = delete;
  ProfileWriter& operator=(const ProfileWriter&) = delete;

  // These functions return true if the corresponding model has been loaded.
  // If the models haven't been loaded, the importer waits to run until they've
  // completed.
  virtual bool BookmarkModelIsLoaded() const;
  virtual bool TemplateURLServiceIsLoaded() const;

  // Helper methods for adding data to local stores.
  virtual void AddPasswordForm(const password_manager::PasswordForm& form);

  virtual void AddHistoryPage(const history::URLRows& page,
                              history::VisitSource visit_source);

  virtual void AddHomepage(const GURL& homepage);

  // Adds the |bookmarks| to the bookmark model.
  //
  // (a) If the bookmarks bar is empty:
  //     (i) If |bookmarks| includes at least one bookmark that was originally
  //         located in a toolbar, all such bookmarks are imported directly to
  //         the toolbar; any other bookmarks are imported to a subfolder in
  //         the toolbar.
  //     (i) If |bookmarks| includes no bookmarks that were originally located
  //         in a toolbar, all bookmarks are imported directly to the toolbar.
  // (b) If the bookmarks bar is not empty, all bookmarks are imported to a
  //     subfolder in the toolbar.
  //
  // In either case, if a subfolder is created, the name will be the value of
  // |top_level_folder_name|, unless a folder with this name already exists.
  // If a folder with this name already exists, then the name is uniquified.
  // For example, if |first_folder_name| is 'Imported from IE' and a folder with
  // the name 'Imported from IE' already exists in the bookmarks toolbar, then
  // we will instead create a subfolder named 'Imported from IE (1)'.
  virtual void AddBookmarks(const std::vector<ImportedBookmarkEntry>& bookmarks,
                            const std::u16string& top_level_folder_name);
  
  // Ecosia: Bookmark Import / Export
  virtual void AddBookmarksWithModel(
      bookmarks::BookmarkModel* model,
      const std::vector<ImportedBookmarkEntry>& bookmarks,
      const std::u16string& top_level_folder_name);

  virtual void AddFavicons(const favicon_base::FaviconUsageDataList& favicons);

  // Adds the TemplateURLs in |template_urls| to the local store.
  // Some TemplateURLs in |template_urls| may conflict (same keyword or same
  // host name in the URL) with existing TemplateURLs in the local store, in
  // which case the existing ones take precedence and the duplicates in
  // |template_urls| are deleted. If |unique_on_host_and_path| is true, a
  // TemplateURL is only added if there is not an existing TemplateURL that has
  // a replaceable search url with the same host+path combination.
  virtual void AddKeywords(
      TemplateURLService::OwnedTemplateURLVector template_urls,
      bool unique_on_host_and_path);

  // Adds the imported autofill entries to the autofill database.
  virtual void AddAutofillFormDataEntries(
      const std::vector<autofill::AutofillEntry>& autofill_entries);

 protected:
  friend class base::RefCountedThreadSafe<ProfileWriter>;

  virtual ~ProfileWriter();

 private:
  const raw_ptr<Profile> profile_;
};

#endif  // CHROME_BROWSER_IMPORTER_PROFILE_WRITER_H_
