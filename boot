#!/usr/bin/env perl
# A bootstrap quasi-interpreter that suffices to coerce the self-code into a
# working C program. Basically an external JIT host for the first (and lame)
# compilation cycle.

open my $fh, '< src/self.canard.sdoc';
my $self = join "\n", grep /^\s*[^\s|A-Z]/, split /\n\n/, join '', <$fh>;
close $fh;

our %native_offsets;
our %native_names;
our @parsed = (our $nil = $native_offsets{''} = 0);
our @heap;

sub cons {
  my ($t, $h) = @_;
  my $r = @heap;
  push @heap, [$t, $h], (0) x 7;
  $r | 2 << 30;
}

sub h {$heap[$_[0] & 0x3fffffff][1]}
sub t {$heap[$_[0] & 0x3fffffff][0]}

sub str;
sub str {
  my $t = $_[0] >> 30;
  my $v = $_[0] & 0x3fffffff;
  return "!$native_names{$v}"       if $t == 0;
  return pack('l', $v) =~ s/\0*$//r if $t == 1;
  return $heap[$v]                  if $t == 3;

  my @xs;
  $v = $_[0];
  while ($v >> 30 == 2) {
    push @xs, str h $v;
    $v = t $v;
  }
  "[" . join(" ", map $xs[-$_], 1..@xs) . "]";
}

while ($self =~ /\G\s*(\[|\]|"#(\S+)\n([\s\S]*?)\n\2|[^]["\s][^][\s]*)/g) {
  my ($v, $h, $s) = ($1, $2, $3);
  if ($v eq '[') {
    push @parsed, $nil;
  } elsif ($v eq ']') {
    my $h = pop @parsed;
    $parsed[-1] = cons $parsed[-1], $h;
  } elsif (length $h) {
    my $h = @heap;
    push @heap, $s, (0) x (length($s) + 3);
    $parsed[-1] = cons $parsed[-1], $h | 3 << 30;
  } elsif ($v =~ /^!(.*)/) {
    $parsed[-1] = cons $parsed[-1],
                       $native_offsets{$1} = keys %native_offsets;
    $native_names{$native_offsets{$1}} = $1;
  } else {
    # Packed-immediate symbol encoding
    $parsed[-1] = cons $parsed[-1],
                       1 << 30 | 0x3fffffff & unpack 'l', "$v\0\0\0";
  }
}

die "mismatched brackets: " . scalar @parsed unless @parsed == 1;

my %defs;
for (my $cons = shift @parsed; $cons; $cons = t $cons) {
  my $l = h $cons;
  $defs{pack('l', 0x3fffffff & h $l) =~ s/\0*$//r} = t $l;
}

printf STDERR "$_ -> %s\n", str $defs{$_} for keys %defs;
