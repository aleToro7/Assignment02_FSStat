class Stopwatch {
  private startTime: bigint = 0n;
  private started: boolean = false;

  private constructor() {}

  static startNew(): Stopwatch {
    return new Stopwatch().start();
  }

  private start(): Stopwatch {
    this.started = true;
    this.startTime = process.hrtime.bigint();
    return this;
  }

  stop(): number {
    this.assertStarted();
    const endTime = process.hrtime.bigint();
    const duration = endTime - this.startTime;
    return Number(duration) * 1e-6; // convert to milliseconds
  }

  getElapsedTime(): number {
    this.assertStarted();
    const currentTime = process.hrtime.bigint();
    const duration = currentTime - this.startTime;
    return Number(duration) * 1e-6; // convert to milliseconds
  }

  private assertStarted(): void {
    if (!this.started) {
      throw new Error("StopWatch has not been started");
    }
  }
}

export async function benchmark<T>(
  fn: () => Promise<T>,
): Promise<{ duration: number; result: T }> {
  const stopwatch = Stopwatch.startNew();
  const result = await fn();
  const duration = stopwatch.stop();
  return { duration, result };
}
