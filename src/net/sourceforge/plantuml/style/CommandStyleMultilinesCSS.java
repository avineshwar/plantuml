/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * (C) Copyright 2009-2020, Arnaud Roques
 *
 * Project Info:  http://plantuml.com
 * 
 * If you like this project or if you find it useful, you can support us at:
 * 
 * http://plantuml.com/patreon (only 1$ per month!)
 * http://plantuml.com/paypal
 * 
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 *
 * Original Author:  Arnaud Roques
 *
 *
 */
package net.sourceforge.plantuml.style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.plantuml.SkinParam;
import net.sourceforge.plantuml.StringLocated;
import net.sourceforge.plantuml.UmlDiagram;
import net.sourceforge.plantuml.command.BlocLines;
import net.sourceforge.plantuml.command.CommandExecutionResult;
import net.sourceforge.plantuml.command.CommandMultilines2;
import net.sourceforge.plantuml.command.MultilinesStrategy;
import net.sourceforge.plantuml.command.regex.IRegex;
import net.sourceforge.plantuml.command.regex.Matcher2;
import net.sourceforge.plantuml.command.regex.MyPattern;
import net.sourceforge.plantuml.command.regex.Pattern2;
import net.sourceforge.plantuml.command.regex.RegexConcat;
import net.sourceforge.plantuml.command.regex.RegexLeaf;

public class CommandStyleMultilinesCSS extends CommandMultilines2<UmlDiagram> {

	public static final String STYLE_SELECTOR_SEPARATOR2 = "-&";

	public CommandStyleMultilinesCSS() {
		super(getRegexConcat(), MultilinesStrategy.REMOVE_STARTING_QUOTE);
	}

	@Override
	public String getPatternEnd() {
		return "(?i)^[%s]*\\</style\\>[%s]*$";
	}

	private static IRegex getRegexConcat() {
		return RegexConcat.build(CommandStyleMultilinesCSS.class.getName(), RegexLeaf.start(), //
				new RegexLeaf("\\<style\\>"), //
				RegexLeaf.end() //
				);
	}

	private static final String NAME_USER = "[\\w()]+?";
	private final static Pattern2 userName = MyPattern.cmpile("^[.:]?(" + NAME_USER + ")([%s]+\\*)?[%s]*\\{$");
	private final static Pattern2 propertyAndValue = MyPattern.cmpile("^([\\w]+):?[%s]+(.*?);?$");
	private final static Pattern2 closeBracket = MyPattern.cmpile("^\\}$");

	protected CommandExecutionResult executeNow(UmlDiagram diagram, BlocLines lines) {
		if (SkinParam.USE_STYLES() == false) {
			return CommandExecutionResult.ok();
		}
		final StyleBuilder styleBuilder = diagram.getSkinParam().getCurrentStyleBuilder();
		for (Style modifiedStyle : getDeclaredStyles(lines.subExtract(1, 1), styleBuilder)) {
			diagram.getSkinParam().muteStyle(modifiedStyle);
		}
		return CommandExecutionResult.ok();
	}

	public Collection<Style> getDeclaredStyles(BlocLines lines, AutomaticCounter counter) {
		final List<Style> result = new ArrayList<Style>();

		final List<String> context = new ArrayList<String>();
		final List<Map<PName, Value>> maps = new ArrayList<Map<PName, Value>>();
		boolean inComment = false;
		for (StringLocated s : lines) {
			String trimmed = s.getTrimmed().getString();
			if (trimmed.startsWith("/*") || trimmed.startsWith("/'")) {
				inComment = true;
				continue;
			}
			if (trimmed.endsWith("*/") || trimmed.endsWith("'/")) {
				inComment = false;
				continue;
			}
			if (inComment) {
				continue;
			}
			final int x = trimmed.lastIndexOf("//");
			if (x != -1) {
				trimmed = trimmed.substring(0, x).trim();
			}
			final Matcher2 mUserName = userName.matcher(trimmed);
			if (mUserName.find()) {
				String n = mUserName.group(1);
				final boolean isRecurse = mUserName.group(2) != null;
				if (isRecurse) {
					n += "*";
				}
				context.add(n);
				maps.add(new EnumMap<PName, Value>(PName.class));
				continue;
			}
			final Matcher2 mPropertyAndValue = propertyAndValue.matcher(trimmed);
			if (mPropertyAndValue.find()) {
				final PName key = PName.getFromName(mPropertyAndValue.group(1));
				final String value = mPropertyAndValue.group(2);
				if (key != null) {
					maps.get(maps.size() - 1).put(key, new ValueImpl(value, counter));
				}
				continue;
			}
			final Matcher2 mCloseBracket = closeBracket.matcher(trimmed);
			if (mCloseBracket.find()) {
				if (context.size() > 0) {
					final StyleSignature signature = contextToSignature(context);
					final Style style = new Style(signature, maps.get(maps.size() - 1));
					result.add(style);
					context.remove(context.size() - 1);
					maps.remove(maps.size() - 1);
				}
			}
		}

		return Collections.unmodifiableList(result);

	}

	private StyleSignature contextToSignature(List<String> context) {
		StyleSignature result = StyleSignature.empty();
		boolean star = false;
		for (Iterator<String> it = context.iterator(); it.hasNext();) {
			String s = it.next();
			if (s.endsWith("*")) {
				star = true;
				s = s.substring(0, s.length() - 1);
			}
			result = result.add(s);
		}
		if (star) {
			result = result.addStar();
		}
		return result;
	}

}
