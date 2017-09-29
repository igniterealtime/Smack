.PHONY: all clean

all: smack-logo-plain.png smack-logo-plain-icon-16x16.png smack-logo-plain-icon-32x32.png smack-logo-plain-icon-128x128.png smack-logo-plain-minimized.svg

smack-logo-plain.png: smack-logo-plain.svg
	convert $< $@

smack-logo-plain-icon-16x16.png: smack-logo-plain.svg
	convert -resize 16x16 $< $@

smack-logo-plain-icon-32x32.png: smack-logo-plain.svg
	convert -resize 32x32 $< $@

smack-logo-plain-icon-128x128.png: smack-logo-plain.svg
	convert -resize 128x128 $< $@

smack-logo-plain-minimized.svg: smack-logo-plain.svg
	scour -i $< -o $@ --enable-viewboxing --enable-id-stripping --enable-comment-stripping --shorten-ids --indent=none

clean:
	rm -f *.png
	rm -f smack-logo-plain-minimized.svg
