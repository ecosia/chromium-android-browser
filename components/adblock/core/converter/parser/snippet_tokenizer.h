/*
 * This file is part of eyeo Chromium SDK,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * eyeo Chromium SDK is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * eyeo Chromium SDK is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with eyeo Chromium SDK.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef COMPONENTS_ADBLOCK_CORE_CONVERTER_PARSER_SNIPPET_TOKENIZER_H_
#define COMPONENTS_ADBLOCK_CORE_CONVERTER_PARSER_SNIPPET_TOKENIZER_H_

#include <string>
#include <vector>

#include "base/strings/string_piece.h"

namespace adblock {

class SnippetTokenizer {
 public:
  using SnippetScript = std::vector<std::vector<std::string>>;

  SnippetTokenizer();
  ~SnippetTokenizer();

  SnippetScript Tokenize(base::StringPiece input);

 private:
  std::vector<std::string> arguments_;
  std::string token_;
  bool escape_ = false;
  bool quotes_just_closed_ = false;
  bool within_quotes_ = false;

  void AddEscape(const char ch);
  void AddArgument();
  void AddFunctionCall(SnippetScript& script);
};

}  // namespace adblock

#endif  // COMPONENTS_ADBLOCK_CORE_CONVERTER_PARSER_SNIPPET_TOKENIZER_H_
