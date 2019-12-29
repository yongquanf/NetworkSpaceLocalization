/*
 * Copyright 2008 Jonathan Ledlie and Peter Pietzuch
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * @author Last modified by $Author: ledlie $
 * @version $Revision: 1.2 $ on $Date: 2008/03/27 18:33:01 $
 * @since Mar 13, 2007
 */
package edu.harvard.syrah.pyxida.knn;

import edu.NUDT.pdl.Nina.StableNC.lib.Coordinate;

public class NodeDesc {

	public final int id;
	public final Coordinate coord;

	public NodeDesc(int _id, Coordinate _coord) {
		id = _id;
		coord = _coord;
	}

	// used when adding to Set b/c one guy can be in the lists several times
	public boolean equals(NodeDesc _other) {
		if (id == _other.id)
			return true;
		return false;
	}

}
