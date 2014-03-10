/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.codeInspection.spellchecker;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.codeInspection.GroovySuppressableInspectionTool;
import org.jetbrains.plugins.groovy.lang.lexer.TokenSets;
import org.jetbrains.plugins.groovy.lang.psi.GrNamedElement;
import org.jetbrains.plugins.groovy.lang.psi.util.GrStringUtil;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer;
import com.intellij.spellchecker.tokenizer.SuppressibleSpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.TokenConsumer;
import com.intellij.spellchecker.tokenizer.Tokenizer;

/**
 * @author peter
 */
public class GroovySpellcheckingStrategy extends SuppressibleSpellcheckingStrategy
{
	private final GrDocCommentTokenizer myDocCommentTokenizer = new GrDocCommentTokenizer();
	private final Tokenizer<PsiElement> myStringTokenizer = new Tokenizer<PsiElement>()
	{
		@Override
		public void tokenize(@NotNull PsiElement literal, TokenConsumer consumer)
		{
			String text = GrStringUtil.removeQuotes(literal.getText());
			if(!text.contains("\\"))
			{
				consumer.consumeToken(literal, PlainTextSplitter.getInstance());
			}
			else
			{
				StringBuilder unescapedText = new StringBuilder();
				int[] offsets = new int[text.length() + 1];
				GrStringUtil.parseStringCharacters(text, unescapedText, offsets);
				EscapeSequenceTokenizer.processTextWithOffsets(literal, consumer, unescapedText, offsets, GrStringUtil.getStartQuote(literal.getText
						()).length());
			}
		}
	};

	@NotNull
	@Override
	public Tokenizer getTokenizer(PsiElement element)
	{
		if(TokenSets.STRING_LITERAL_SET.contains(element.getNode().getElementType()))
		{
			return myStringTokenizer;
		}
		if(element instanceof GrNamedElement)
		{
			final PsiElement name = ((GrNamedElement) element).getNameIdentifierGroovy();
			if(TokenSets.STRING_LITERAL_SET.contains(name.getNode().getElementType()))
			{
				return EMPTY_TOKENIZER;
			}
		}
		if(element instanceof PsiDocComment)
		{
			return myDocCommentTokenizer;
		}
		//if (element instanceof GrLiteralImpl && ((GrLiteralImpl)element).isStringLiteral()) return myStringTokenizer;
		return super.getTokenizer(element);
	}

	@Override
	public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String name)
	{
		return GroovySuppressableInspectionTool.getElementToolSuppressedIn(element, name) != null;
	}

	@Override
	public SuppressQuickFix[] getSuppressActions(@NotNull PsiElement element, @NotNull String name)
	{
		return GroovySuppressableInspectionTool.getSuppressActions(name);
	}
}
