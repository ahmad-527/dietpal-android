package com.example.dietpal.planning;

/**
 * An explicit user constraint. Ranges and limits are strict; point goals use the shown 10%
 * tolerance.
 */
public final class Goal {
  public String kind = "off";
  public double lower, upper;
  public boolean exclusive;

  public static Goal point(double value) {
    Goal goal = new Goal();
    if (value > 0) {
      goal.kind = "point";
      goal.lower = value;
      goal.upper = value;
    }
    return goal;
  }

  public static Goal parse(String input, boolean optional) {
    String text =
        input
            .trim()
            .replace(',', '.')
            .replace("≥", ">=")
            .replace("≤", "<=")
            .replace('–', '-')
            .replace('—', '-')
            .replaceAll("\\s+", "");
    if (text.isEmpty()) {
      if (optional) return new Goal();
      throw new IllegalArgumentException("Enter all three macro targets.");
    }
    Goal out = new Goal();
    String number = "[0-9]+(?:\\.[0-9]+)?";
    if (text.matches(number)) {
      out.kind = "point";
      out.lower = out.upper = Double.parseDouble(text);
    } else if (text.matches(number + "-" + number)) {
      String[] ends = text.split("-");
      out.kind = "range";
      out.lower = Double.parseDouble(ends[0]);
      out.upper = Double.parseDouble(ends[1]);
    } else if (text.matches("[<>]=?" + number)) {
      boolean minimum = text.charAt(0) == '>';
      double value = Double.parseDouble(text.replaceAll("^[<>]=?", ""));
      out.kind = minimum ? "min" : "max";
      out.exclusive = !text.contains("=");
      if (minimum) out.lower = value;
      else out.upper = value;
    } else
      throw new IllegalArgumentException(
          "Use a value, a range such as 100-150, or a limit such as >2000 or <=2000.");
    out.validate(optional);
    return out;
  }

  public void validate(boolean optional) {
    if (kind.equals("off")) {
      if (!optional) throw new IllegalArgumentException("Enter all three macro targets.");
      return;
    }
    if (!java.util.Arrays.asList("point", "range", "min", "max").contains(kind)
        || !Double.isFinite(lower)
        || !Double.isFinite(upper)
        || lower < 0
        || upper < 0
        || lower > 20000
        || upper > 20000
        || aim() <= 0
        || (kind.equals("range") && upper < lower))
      throw new IllegalArgumentException(
          "Enter positive, ordered goals below 20,000. A goal is not a medical recommendation.");
  }

  public boolean enabled() {
    return !kind.equals("off");
  }

  public double aim() {
    return kind.equals("range") ? (lower + upper) / 2 : kind.equals("max") ? upper : lower;
  }

  public boolean accepts(double value) {
    return distance(value) == 0;
  }

  public double distance(double value) {
    if (!enabled()) return 0;
    double min = kind.equals("max") ? 0 : kind.equals("point") ? lower * .9 : lower;
    double max =
        kind.equals("min") ? Double.POSITIVE_INFINITY : kind.equals("point") ? upper * 1.1 : upper;
    if (exclusive && kind.equals("min")) min = Math.nextUp(min);
    if (exclusive && kind.equals("max")) max = Math.nextDown(max);
    return value < min
        ? (value - min) / Math.max(1, aim())
        : value > max ? (value - max) / Math.max(1, aim()) : 0;
  }

  private String number(double value) {
    return java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
  }

  public String raw() {
    if (kind.equals("off")) return "";
    if (kind.equals("range")) return number(lower) + "-" + number(upper);
    if (kind.equals("min")) return (exclusive ? ">" : ">=") + number(lower);
    if (kind.equals("max")) return (exclusive ? "<" : "<=") + number(upper);
    return number(lower);
  }

  public String label() {
    return enabled() ? raw() : "Not set";
  }
}
