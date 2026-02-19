package com.example.copycoords;

/**
 * Utility class for calculating distance and direction between two coordinate sets
 */
public class DistanceCalculator {
    
    /**
     * Represents calculation result with distance and bearing information
     */
    public static class DistanceResult {
        public final double horizontalDistance;  // Distance on X-Z plane
        public final double verticalDistance;    // Difference in Y
        public final double totalDistance;       // 3D Euclidean distance
        public final double bearing;             // Angle in degrees (0-360), 0 = North, 90 = East, 180 = South, 270 = West
        public final String direction;           // Cardinal direction (N, NE, E, SE, S, SW, W, NW)
        public final int blocksTravelledHorizontal; // Blocks to travel horizontally (Manhattan)
        
        public DistanceResult(double horizontalDistance, double verticalDistance, double totalDistance, 
                            double bearing, String direction, int blocksTravelledHorizontal) {
            this.horizontalDistance = horizontalDistance;
            this.verticalDistance = verticalDistance;
            this.totalDistance = totalDistance;
            this.bearing = bearing;
            this.direction = direction;
            this.blocksTravelledHorizontal = blocksTravelledHorizontal;
        }
    }
    
    /**
     * Calculate distance and bearing between two coordinate sets
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param z1 Starting Z coordinate
     * @param x2 Destination X coordinate
     * @param y2 Destination Y coordinate
     * @param z2 Destination Z coordinate
     * @return DistanceResult containing all relevant information
     */
    public static DistanceResult calculate(int x1, int y1, int z1, int x2, int y2, int z2) {
        // Calculate differences
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        
        // Calculate distances
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double verticalDistance = dy;
        double totalDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Calculate bearing (angle from north)
        // In Minecraft: +Z = South, -Z = North, +X = East, -X = West
        double bearing = calculateBearing(dx, dz);
        
        // Get cardinal direction
        String direction = getCardinalDirection(bearing);
        
        // Calculate Manhattan distance (blocks traveled horizontally)
        int blocksTravelledHorizontal = (int) (Math.abs(dx) + Math.abs(dz));
        
        return new DistanceResult(horizontalDistance, verticalDistance, totalDistance, bearing, 
                                direction, blocksTravelledHorizontal);
    }
    
    /**
     * Calculate bearing in degrees from one point to another
     * Returns 0-360 degrees where:
     * 0° = North (-Z direction)
     * 90° = East (+X direction)
     * 180° = South (+Z direction)
     * 270° = West (-X direction)
     * @param dx Change in X
     * @param dz Change in Z
     * @return Bearing in degrees (0-360)
     */
    private static double calculateBearing(double dx, double dz) {
        // Use atan2 to get angle. In Minecraft, Z is the vertical axis on the horizontal plane
        // atan2(dz, dx) because we measure from north and east
        double angleRadians = Math.atan2(dx, -dz);  // -dz because north is -Z
        double angleDegrees = Math.toDegrees(angleRadians);
        
        // Normalize to 0-360 range
        if (angleDegrees < 0) {
            angleDegrees += 360;
        }
        
        return angleDegrees;
    }
    
    /**
     * Get cardinal direction from bearing angle
     * @param bearing Bearing in degrees (0-360)
     * @return Cardinal direction (N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW)
     */
    public static String getCardinalDirection(double bearing) {
        // Use 16-point compass
        String[] directions = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", 
                             "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};
        
        // Offset bearing by 11.25 degrees so that each direction spans its full 22.5 degree range
        double offset = bearing + 11.25;
        if (offset >= 360) {
            offset -= 360;
        }
        
        int index = (int) (offset / 22.5);
        return directions[index % 16];
    }
    
    /**
     * Format the result for display in chat
     * @param result The distance calculation result
     * @param showAll Whether to show all information or just summary
     * @return Formatted string for chat
     */
    public static String formatResult(DistanceResult result, boolean showAll) {
        String format;
        if (showAll) {
            format = String.format("Distance: %.1f blocks (horizontal), %.1f blocks vertically, %.1f blocks total | " +
                    "Direction: %s (%.1f°) | Blocks to travel: %d",
                    result.horizontalDistance,
                    result.verticalDistance,
                    result.totalDistance,
                    result.direction,
                    result.bearing,
                    result.blocksTravelledHorizontal);
        } else {
            format = String.format("Distance: %.1f blocks | Direction: %s (%.1f°)",
                    result.horizontalDistance,
                    result.direction,
                    result.bearing);
        }
        return format;
    }
    
    /**
     * Check if coordinates are in a nether-equivalent scale and adjust if needed
     * @param x1 Starting X
     * @param z1 Starting Z
     * @param x2 Destination X
     * @param z2 Destination Z
     * @param sourceIsDimension Whether source dimension
     * @return Adjusted coordinates or original if no adjustment needed
     */
    public static int[] adjustForDimensionScale(int x1, int z1, int x2, int z2, boolean sourceIsNether) {
        // If comparing nether to overworld, normalize to overworld coordinates
        // This helps with cross-dimension distance calculations
        if (sourceIsNether) {
            return new int[]{x1 * 8, z1 * 8, x2, z2};
        } else {
            return new int[]{x1, z1, x2 / 8, z2 / 8};
        }
    }
}
