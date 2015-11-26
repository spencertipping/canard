#!/usr/bin/env perl
# A bootstrap quasi-interpreter that suffices to coerce the self-code into a
# working C program. Basically an external JIT host for the first (and lame)
# compilation cycle.

open my $fh, '< src/self.canard.sdoc';
my $self = join "\n", grep /^\s*[^\s|A-Z]/, split /\n\n/, join '', <$fh>;
close $fh;

my %native_offsets;

# Reserve a space for nil
my $nil    = $native_offsets{''} = 0;
my @parsed = $nil;
my @heap;

while ($self =~ /\G\s*(\[|\]|"#(.+)\n([\s\S]*?)\n\3|[^]["\s][^][\s]*)/g) {
  my ($v, $h, $s) = ($1, $2, $3);
  if ($v eq '[') {
    push @parsed, $nil;
  } elsif ($v eq ']') {
    my $h = pop @parsed;
    my $t = pop @parsed;
    push @heap, [$t, $h], (0) x 7;
    push @parsed, $#heap | 2 << 30;
  } elsif ($v =~ /^"#/) {
    push @heap, $s, (0) x length($s) + 3;
    push @parsed, $#heap | 3 << 30;
  } elsif ($v =~ /^!(.*)/) {
    push @parsed, $native_offsets{substr $1, 1} = keys %native_offsets;
  } else {
    # Packed-immediate symbol encoding
    push @parsed, 1 << 30 | 0x3fffffff & unpack "l", $1;
  }
}

die "mismatched brackets: " . scalar @parsed unless @parsed == 1;
