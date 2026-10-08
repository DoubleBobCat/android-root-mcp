import 'package:flutter/material.dart';

/// The selected ARMCP Node Bridge mark, drawn without a runtime image plugin.
class ArmcpBrandMark extends StatelessWidget {
  const ArmcpBrandMark({super.key, this.size = 40});

  final double size;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(painter: _NodeBridgePainter()),
    );
  }
}

class _NodeBridgePainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final scale = size.width / 1024;
    canvas.scale(scale, scale);
    final bounds = Rect.fromLTWH(0, 0, 1024, 1024);
    final background = Paint()..color = const Color(0xFF122A42);
    canvas.drawRRect(
      RRect.fromRectAndRadius(bounds, const Radius.circular(250)),
      background,
    );

    final line = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 28
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round
      ..shader = const LinearGradient(
        colors: [Color(0xFF63E6D5), Color(0xFF8092FF)],
      ).createShader(bounds);
    final bridge = Path()
      ..moveTo(254, 336)
      ..lineTo(512, 512)
      ..lineTo(770, 336)
      ..moveTo(254, 688)
      ..lineTo(512, 512)
      ..lineTo(770, 688)
      ..moveTo(254, 336)
      ..lineTo(254, 688)
      ..moveTo(770, 336)
      ..lineTo(770, 688);
    canvas.drawPath(bridge, line);

    final node = Paint()..color = const Color(0xFF0B1829);
    final teal = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 22
      ..color = const Color(0xFF63E6D5);
    final violet = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 22
      ..color = const Color(0xFF8092FF);
    canvas
      ..drawCircle(const Offset(254, 336), 79, node)
      ..drawCircle(const Offset(254, 336), 79, teal)
      ..drawCircle(const Offset(770, 336), 79, node)
      ..drawCircle(const Offset(770, 336), 79, violet)
      ..drawCircle(const Offset(254, 688), 79, node)
      ..drawCircle(const Offset(254, 688), 79, violet)
      ..drawCircle(const Offset(770, 688), 79, node)
      ..drawCircle(const Offset(770, 688), 79, teal);

    canvas.drawCircle(const Offset(512, 512), 102, node);
    final center = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 20
      ..color = const Color(0xFFF2F7FF);
    canvas.drawCircle(const Offset(512, 512), 102, center);
    final plus = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 20
      ..strokeCap = StrokeCap.round
      ..color = const Color(0xFFF2F7FF);
    canvas.drawLine(const Offset(472, 512), const Offset(552, 512), plus);
    canvas.drawLine(const Offset(512, 472), const Offset(512, 552), plus);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
