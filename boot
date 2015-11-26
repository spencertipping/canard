#!/usr/bin/env perl
# A bootstrap quasi-interpreter that suffices to coerce the self-code into a
# working C program. Basically an external JIT host for the first (and lame)
# compilation cycle.

open my $fh, '< src/self.canard.sdoc';
my $self = join "\n", grep /^\s*[^\s|A-Z]/, split /\n\n/, join '', <$fh>;
close $fh;

my %native_offsets;
my @parsed = 0;
my @heap;

while ($self =~ /\G\s*("#(.+)\n([\s\S]*?)\n\3|[^]["\s][^][\s]*|\[|\])/g) {
  if ($1 eq '[') {
    push @parsed, 0;
  } elsif ($1 eq ']') {
    my $h = pop @parsed;
    my $t = pop @parsed;
    push @heap, [$t, $h], (0) x 7;
    push @parsed, $#heap | 2 << 30;
  } elsif ($1 =~ /^"/) {
    push @heap, $1, (0) x length($1) - 1;
    push @parsed, $#heap | 3 << 30;
  } elsif ($1 =~ /^!/) {
    # TODO: pre-resolved native ref
  } else {
    # Packed-immediate symbol encoding
    push @parsed, 1 << 30 | 0x3fffffff & unpack "l", $1;
  }
}
