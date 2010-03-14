/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.serialisation.dotg;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.workcraft.Plugin;
import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.plugins.petri.PetriNetModel;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.stg.STGModel;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.SignalTransition.Type;
import org.workcraft.serialisation.Format;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.ReferenceProducer;

public class DotGSerialiser implements ModelSerialiser, Plugin {
	class ReferenceResolver implements ReferenceProducer {
		HashMap<Object, String> refMap = new HashMap<Object, String>();

		public String getReference(Object obj) {
			return refMap.get(obj);
		}
	}

	private void writeSignalsHeader (PrintWriter out, Collection<String> signalNames, String header) {
		if (signalNames.isEmpty())
			return;

		LinkedList<String> sortedNames = new LinkedList<String>(signalNames);
		Collections.sort(sortedNames);

		out.print(header);

		for (String s : sortedNames) {
			out.print(" ");
			out.print(s);
		}

		out.print("\n");
	}

	private void writeGraphEntry(PrintWriter out, Model model, Node node) {
		out.write(model.getNodeReference(node));

		for (Node n : model.getPostset(node)) {
			out.write(" ");
			out.write(model.getNodeReference(n));
		}

		out.write("\n");
	}

	public ReferenceProducer serialise(Model model, OutputStream outStream, ReferenceProducer inRef) {
		PrintWriter out = new PrintWriter(outStream);
		out.print("# STG file generated by Workcraft.\n");

		ReferenceResolver resolver = new ReferenceResolver();

		if (model instanceof STGModel)
			writeSTG((STGModel)model, out);
		else if (model instanceof PetriNetModel)
			writePN((PetriNetModel)model, out);
		else
			throw new ArgumentException ("Model class not supported: " + model.getClass().getName());

		out.print(".end\n");

		out.close();

		return resolver;
	}

	private void writeSTG(STGModel stg, PrintWriter out) {
		writeSignalsHeader(out, stg.getSignalNames(Type.INTERNAL), ".internal");
		writeSignalsHeader(out, stg.getSignalNames(Type.INPUT), ".inputs");
		writeSignalsHeader(out, stg.getSignalNames(Type.OUTPUT), ".outputs");
		writeSignalsHeader(out, stg.getDummyNames(), ".dummy");

		out.print(".graph\n");

		for (SignalTransition t : stg.getSignalTransitions())
			writeGraphEntry (out, stg, t);

		for (Transition t : stg.getDummies())
			writeGraphEntry (out, stg, t);

		for (Place p : stg.getPlaces())
			writeGraphEntry (out, stg, p);

		writeMarking(stg, stg.getPlaces(), out);
	}

	private void writeMarking(Model model, Collection<Place> places, PrintWriter out) {
		out.print(".marking {");

		for (Place p: places) {
			final int tokens = p.getTokens();
			if (tokens == 1)
				out.print(" " + model.getNodeReference(p));
			else if (tokens > 1)
				out.print(" " + model.getNodeReference(p) + "=" + tokens);
		}

		out.print (" }\n");

		StringBuilder capacity = new StringBuilder();

		for (Place p : places) {
			if (p.getCapacity() != 1)
				capacity.append(" " + model.getNodeReference(p) + "=" + p.getCapacity());
		}

		if (capacity.length() > 0)
			out.print(".capacity" + capacity + "\n");
	}

	private void writePN(PetriNetModel net, PrintWriter out) {
		LinkedList<String> transitions = new LinkedList<String>();

		for (Transition t : net.getTransitions())
			transitions.add(net.getNodeReference(t));

		writeSignalsHeader(out, transitions, ".dummy");

		out.print(".graph\n");

		for (Transition t : net.getTransitions())
			writeGraphEntry (out, net, t);

		for (Place p : net.getPlaces())
			writeGraphEntry (out,net, p);

		writeMarking(net, net.getPlaces(), out);
	}

	public boolean isApplicableTo(Model model) {
		if (model instanceof STGModel || model instanceof PetriNetModel)
			return true;
		return false;
	}

	public String getDescription() {
		return "Workcraft STG serialiser";
	}

	public String getExtension() {
		return ".g";
	}

	public UUID getFormatUUID() {
		return Format.STG;
	}
}