/**
 * Command Execution Context
 * Provides parsed parameters and output capabilities for command execution
 */
export class CmdExecutionContext {
  private parameters: string[];
  private rawInput: string;
  private output: string[] = [];

  constructor(rawInput: string, parameters: string[]) {
    this.rawInput = rawInput;
    this.parameters = parameters;
  }

  /**
   * Get parameter at index
   */
  getParameter(index: number): string | undefined {
    return this.parameters[index];
  }

  /**
   * Get all parameters
   */
  getParameters(): string[] {
    return [...this.parameters];
  }

  /**
   * Get parameter count
   */
  getParameterCount(): number {
    return this.parameters.length;
  }

  /**
   * Get raw input string
   */
  getRawInput(): string {
    return this.rawInput;
  }

  /**
   * Write line to output
   */
  writeLine(message: string): void {
    this.output.push(message);
  }

  /**
   * Write error line to output
   */
  writeError(message: string): void {
    this.output.push(`[ERROR] ${message}`);
  }

  /**
   * Get all output lines
   */
  getOutput(): string[] {
    return [...this.output];
  }

  /**
   * Clear output
   */
  clearOutput(): void {
    this.output = [];
  }
}
