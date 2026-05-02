const kbUnit = 1024;

/**
 * Utility object for converting file sizes to bytes.
 */
export const FileSize = {
  /**
   * Converts a given number of kilobytes to bytes.
   * @param n The number of kilobytes.
   * @returns The equivalent number of bytes.
   */
  kilobyte: (n: number) => n * kbUnit,
  /**
   * Converts a given number of megabytes to bytes.
   * @param n The number of megabytes.
   * @returns The equivalent number of bytes.
   */
  megabyte: (n: number) => n * kbUnit ** 2,
  /**
   * Converts a given number of gigabytes to bytes.
   * @param n The number of gigabytes.
   * @returns The equivalent number of bytes.
   */
  gigabyte: (n: number) => n * kbUnit ** 3,
  /**
   * Converts a given number of terabytes to bytes.
   * @param n The number of terabytes.
   * @returns The equivalent number of bytes.
   */
  terabyte: (n: number) => n * kbUnit ** 4,
};
