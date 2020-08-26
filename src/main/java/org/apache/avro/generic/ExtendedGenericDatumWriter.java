package org.apache.avro.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.NullNode;

import org.apache.avro.Schema;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.ExtendedJsonEncoder;
import org.apache.avro.io.parsing.Parser;
import org.apache.avro.io.parsing.Symbol;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.internal.JacksonUtils;

public class ExtendedGenericDatumWriter<D> extends GenericDatumWriter<D> {

	public ExtendedGenericDatumWriter(final GenericData data) {
		super(data);
	}

	public ExtendedGenericDatumWriter(final Schema root) {
		super(root);
	}

	public ExtendedGenericDatumWriter(final Schema root, final GenericData data) {
		super(root, data);
	}

	public ExtendedGenericDatumWriter(Class<D> c) {
		super(SpecificData.get().getSchema(c), SpecificData.getForClass(c));
	}

	private static final ThreadLocal<List<Symbol>> HOLDINGS = ThreadLocal.withInitial(() -> new ArrayList<>(8));

	/**
	 * Overwritten to skip serializing fields that have default values.
	 */
	@Override
	protected void writeField(final Object datum, final Schema.Field f, final Encoder out, final Object state)
		throws IOException {
		GenericData data = getData();
		Object defaultValue = JacksonUtils.toJsonNode(f.defaultVal());
		if (defaultValue instanceof NullNode) {
			defaultValue = null;
		}
		Object value = data.getField(datum, f.name(), f.pos());

		if (Objects.equals(value, defaultValue) && out instanceof ExtendedJsonEncoder) {
			skipNullField(((ExtendedJsonEncoder) out).getParser(), f);
		} else {
			super.writeField(datum, f, out, state);
		}
	}

	private void skipNullField(Parser parser, final Schema.Field f) throws IOException {
		Symbol topSymbol = parser.topSymbol();
		switch (topSymbol.kind) {
			case TERMINAL:
			case IMPLICIT_ACTION:
				break;
			default:
				// expand production
				final Symbol nextSymbol = topSymbol.production[topSymbol.production.length - 1];
				if (nextSymbol instanceof Symbol.ImplicitAction) {
					if (((Symbol.ImplicitAction) nextSymbol).isTrailing) {
						throw new IllegalStateException("Cannot start with a trailing implicit"
														+ topSymbol);
					} else {
						parser.advance(nextSymbol);
					}
				} else {
					throw new IllegalStateException("Invalid state " + topSymbol);
				}
				parser.pushSymbol(nextSymbol);
		}
		List<Symbol> holdings = HOLDINGS.get();
		holdings.clear();
		Symbol advanceTo = null;
		boolean done = false;
		while (!done) {
			if (parser.depth() > 0) {
				advanceTo = parser.popSymbol();
				if (advanceTo instanceof Symbol.FieldAdjustAction
					&& ((Symbol.FieldAdjustAction) advanceTo).fname.equals(f.name())
					&& ((Symbol.FieldAdjustAction) advanceTo).rindex == f.pos()) {
					done = true;
				}
				holdings.add(advanceTo);
			} else {
				done = true;
			}
		}
		for (int i = holdings.size() - 1; i >= 0; i--) {
			parser.pushSymbol(holdings.get(i));
		}
		parser.advance(advanceTo);
		int count = 1;
		while (count > 0) {
			Symbol currentSymbol = parser.popSymbol();
			if (currentSymbol.getClass() == Symbol.ImplicitAction.class) {
				if (((Symbol.ImplicitAction) currentSymbol).isTrailing) {
					count--;
				} else {
					count++;
				}
			}
		}
	}
}
