package de.fernunihagen.dna.jkn.scalephant.storage.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fernunihagen.dna.jkn.scalephant.storage.sstable.SSTableHelper;

public class BoundingBox implements Comparable<BoundingBox> {
	
	public final static BoundingBox EMPTY_BOX = new BoundingBox();
	
	/**
	 * The boundingBox contains a bounding box for a tuple.
	 * The boundingBox for n dimensions is structured as follows:
	 * 
	 * boundingBox[0] = coordinate_0
	 * boundingBox[1] = extent_0
	 * boundingBox[2] = coordinate_1
	 * boundingBox[3] = extent_1
	 * boundingBox[4] = coordinate_2
	 * boundingBox[5] = extent_2
	 * 
	 * [...]
	 * boundingBox[2n] = coordinate_n
	 * boundingBox[2n+1] = extent_n
	 */
	protected final List<Float> boundingBox;
	
	/**
	 * The return value of an invalid dimension
	 */
	public final static int INVALID_DIMENSION = -1;
	
	/**
	 * The Logger
	 */
	protected static final Logger logger = LoggerFactory.getLogger(BoundingBox.class);


	/**
	 * Is the bounding box valid?
	 */
	protected final boolean valid;
	
	public BoundingBox(Float... args) {
		boundingBox = new ArrayList<Float>(args.length);
		boundingBox.addAll(Arrays.asList(args));
		valid = checkValid();
	}
	
	public BoundingBox(float[] values) {
		boundingBox = new ArrayList<Float>(values.length);
		
		for(int i = 0; i < values.length; i++) {
			boundingBox.add(values[i]);
		}
		
		valid = checkValid();
	}

	/**
	 * Determines if the bounding box is valid or not
	 */
	protected boolean checkValid() {
		
		if (boundingBox.size() % 2 != 0) {
			logger.warn("Found invalid Bounding Box odd amount of arguments " + boundingBox);
			return false;
		}
		
		// No negative extent
		for(int i = 1; i < boundingBox.size(); i=i+2) {
			if(boundingBox.get(i) < 0) {
				logger.warn("Found invalid Bounding Box - nagative extent: " + boundingBox);
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Returns the valid state of the bounding box
	 * 
	 * @return
	 */
	public boolean isValid() {
		return valid;
	}
	
	/**
	 * Returns the size of the bounding box in bytes
	 * 
	 * @return
	 */
	public int getSize() {
		return boundingBox.size();
	}
	
	/**
	 * Convert the bounding box into a byte array
	 * 
	 * @return
	 */
	public byte[] toByteArray() {
		final float[] values = toFloatArray();
		
		return SSTableHelper.floatArrayToIEEE754ByteBuffer(values).array();
	}

	/**
	 * Convert the boudning box into a float array
	 * 
	 * @return
	 */
	public float[] toFloatArray() {
		final float[] values = new float[boundingBox.size()];
		for(int i = 0; i < boundingBox.size(); i++) {
			values[i] = boundingBox.get(i);
		}
		return values;
	}
	
	/**
	 * Read the bounding box from a byte array
	 * @param boxBytes
	 * @return
	 */
	public static BoundingBox fromByteArray(final byte[] boxBytes) {
		final float[] floatArray = SSTableHelper.readIEEE754FloatArrayFromByte(boxBytes);
		return new BoundingBox(floatArray);
	}
	
	/**
	 * Tests if two bounding boxes share some space
	 * 
	 * For each dimension:
	 * 
	 * Case 1: 1 overlaps 2 at the left end
	 *  |--------|                      // 1
	 *      |------------|              // 2
	 *
	 * Case 2: 1 overlaps 2 at the tight end
	 *            |--------|            // 1
	 *   |------------|                 // 2
	 *
	 * Case 3: 1 is inside 2
	 *    |-------------------|         // 1
	 *  |-----------------------|       // 2
	 *
	 * Case 4: 2 is inside 1
	 * |-----------------------|        // 1
	 *      |----------|                // 2
	 *
	 * Case 5: 1 = 2
	 *            |--------|            // 1
	 *            |--------|            // 2
	 * 
	 * Case 6: No overlapping
	 * |-------|                        // 1
	 *               |---------|        // 2
	 * @param boundingBox
	 * @return
	 */
	public boolean overlaps(final BoundingBox boundingBox) {
		
		// Null does overlap with nothing
		if(boundingBox == null) {
			return false;
		}
		
		// The empty bounding box overlaps everything
		if(boundingBox == BoundingBox.EMPTY_BOX) {
			return true;
		}
		
		// Both boxes are equal (Case 5)
		if(equals(boundingBox)) {
			return true;
		}
		
		// Dimensions are not equal
		if(boundingBox.getDimension() != getDimension()) {
			return false;
		}
		
		// Check the overlapping in each dimension d
		for(int d = 0; d < getDimension(); d++) {
			
			// Case 1 or 3
			if(isCoveringPointInDimension(boundingBox.getCoordinateLow(d), d)) {
				continue;
			}
			
			// Case 2 or 3
			if(isCoveringPointInDimension(boundingBox.getCoordinateHigh(d), d)) {
				continue;
			}
			
			// Case 4 
			if(boundingBox.isCoveringPointInDimension(getCoordinateLow(d), d)) {
				continue;
			}
			
			// None of the above conditions matches (Case 6)
			return false;
		}
		
		return true;
	}
	
	/**
	 * Does the bounding box covers the point in the dimension?
	 * @param point
	 * @param dimension
	 * @return
	 */
	public boolean isCoveringPointInDimension(float point, int dimension) {
		if(getCoordinateLow(dimension) <= point && getCoordinateHigh(dimension) >= point) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * Get the extent for the dimension
	 * @param dimension
	 * @return
	 */
	public float getExtent(final int dimension) {
		return boundingBox.get((2 * dimension) + 1);
	}
	
	/**
	 * The the lowest coordinate for the dimension
	 * @param dimension
	 * @return
	 */
	public float getCoordinateLow(final int dimension) {
		return boundingBox.get(2 * dimension);
	}
	
	/**
	 * The the highest coordinate for the dimension
	 * @param dimension
	 * @return
	 */
	public float getCoordinateHigh(final int dimension) {
		return getCoordinateLow(dimension) + getExtent(dimension);
	}
	
	/**
	 * Return the dimension of the bounding box
	 * @return
	 */
	public int getDimension() {
		
		if(! valid) {
			return INVALID_DIMENSION;
		}
		
		return boundingBox.size() / 2;
	}

	/**
	 * Convert to a readable string
	 * 
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("BoundingBox [dimensions=");
		sb.append(getDimension());
		
		for(int d = 0; d < getDimension(); d++) {
			sb.append(", dimension ");
			sb.append(d);
			sb.append(" low: ");
			sb.append(getCoordinateLow(d));
			sb.append(" high: ");
			sb.append(getCoordinateHigh(d));
		}
				
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Convert into a hashcode
	 * 
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((boundingBox == null) ? 0 : boundingBox.hashCode());
		return result;
	}

	/**
	 * Is equals with an other object
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BoundingBox other = (BoundingBox) obj;
		if (boundingBox == null) {
			if (other.boundingBox != null)
				return false;
		} else if (!boundingBox.equals(other.boundingBox))
			return false;
		return true;
	}

	/**
	 * Compare to an other boudning box
	 */
	@Override
	public int compareTo(final BoundingBox otherBox) {
		
		// Check number od dimensions
		if(getDimension() != otherBox.getDimension()) {
			return getDimension() - otherBox.getDimension(); 
		}
		
		// Check start point of each dimension
		for(int d = 0; d < getDimension(); d++) {
			if(getCoordinateLow(d) != otherBox.getCoordinateLow(d)) {
				if(getCoordinateLow(d) > otherBox.getCoordinateLow(d)) {
					return 1;
				} else {
					return -1;
				}
			}
		}
		
		// Objects are equal
		return 0;
	}
	
	/**
	 * Get the bounding box of two bounding boxes
	 * @param boundingBox1
	 * @param boundingBox2
	 * @return
	 */
	public static BoundingBox getBoundingBox(final BoundingBox... boundingBoxes) {
		
		// No argument
		if(boundingBoxes.length == 0) {
			return null;
		}
		
		// Only 1 argument
		if(boundingBoxes.length == 1) {
			return boundingBoxes[0];
		}
		
		int dimensions = boundingBoxes[0].getDimension();
		
		// All bounding boxes need the same dimension
		for(int i = 1 ; i < boundingBoxes.length; i++) {
			
			final BoundingBox curentBox = boundingBoxes[i];
			
			// Bounding box could be null, e.g. for DeletedTuple instances
			if(curentBox == null) {
				continue;
			}
			
			if(dimensions != curentBox.getDimension()) {
				logger.warn("Merging bounding boxed with different dimensions");
				return null;
			}
		}
		
		// Array with data for the result box
		final float[] coverBox = new float[boundingBoxes[0].getDimension() * 2];
		
		// Construct the covering bounding box
		for(int d = 0; d < dimensions; d++) {
			float resultMin = Float.MAX_VALUE;
			float resultMax = Float.MIN_VALUE;
			
			for(int i = 0; i < boundingBoxes.length; i++) {
				resultMin = Math.min(resultMin, boundingBoxes[i].getCoordinateLow(d));
				resultMax = Math.max(resultMax, boundingBoxes[i].getCoordinateHigh(d));
			}
			
			coverBox[2 * d] = resultMin; // Start position
			coverBox[2 * d + 1] = resultMax - resultMin; // Extend
		}
		
		return new BoundingBox(coverBox);
	}
	
}
