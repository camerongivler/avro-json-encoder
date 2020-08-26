package org.apache.avro.io;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;

import org.apache.avro.Schema;
import org.apache.avro.io.parsing.Parser;
import org.apache.avro.io.parsing.Symbol;

public class ExtendedJsonEncoder extends JsonEncoder implements Parser.ActionHandler {

	ExtendedJsonEncoder(Schema sc, JsonGenerator out) throws IOException {
		super(sc, out);
	}

	@Override
	public void writeIndex(int unionIndex) throws IOException {
		parser.advance(Symbol.UNION);
		Symbol.Alternative top = (Symbol.Alternative) parser.popSymbol();
		Symbol symbol = top.getSymbol(unionIndex);
		parser.pushSymbol(symbol);
	}
}
