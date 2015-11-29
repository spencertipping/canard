#!/usr/bin/env perl
# A bootstrap quasi-interpreter that suffices to coerce the self-code into a
# working C program. Basically an external JIT host for the first (and lame)
# compilation cycle.

open my $fh, '< src/self.canard.sdoc';
my $self = join "\n", grep /^\s*[^\s|A-Z]/, split /\n\n/, join '', <$fh>;
close $fh;

our @parsed = (our $nil = 0);
our @heap;

sub cons {
  my ($t, $h) = @_;
  my $r = @heap;
  push @heap, [$t, $h], (0) x 7;
  $r | 2 << 30;
}

sub val   {$_[0] & 0x3fffffff}
sub fnp   {$_[0] >> 30 == 0}
sub symp  {$_[0] >> 30 == 1}
sub consp {$_[0] >> 30 == 2}
sub strp  {$_[0] >> 30 == 3}

sub h {$heap[val $_[0]][1]}
sub t {$heap[val $_[0]][0]}

sub sname {pack('l', val $_[0]) =~ s/\0*$//r}
sub cname {lc sprintf '_%x', val $_[0]}

sub str;
sub str {
  my ($v) = @_;
  return "!$native_names{val $v}" if fnp $v;
  return sname $v                 if symp $v;
  return $heap[val $v]            if strp $v;

  my @xs;
  while (consp $v) {
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
    push @heap, $s, (0) x (3 + length $s);
    $parsed[-1] = cons $parsed[-1], $h | 3 << 30;
  } else {
    # Packed-immediate symbol encoding
    $parsed[-1] = cons $parsed[-1],
                       1 << 30 | 0x3fffffff & unpack 'l', "$v\0\0\0";
  }
}

die "mismatched brackets: " . scalar @parsed unless @parsed == 1;

my %specials;
my %defs;
++$specials{$_} for qw/ c mc sh /;

for (my $cons = shift @parsed; $cons; $cons = t $cons) {
  my $l    = h $cons;
  my $name = sname h $l;
  $specials{$name} ? $specials{$name}  = t $l
                   : $defs{cname h $l} = t $l;
}

my @natives;
my @native_defs;

for (sort keys %defs) {
  my @xs;
  for (my $cons = $defs{$_}; $cons; $cons = t $cons) {
    unshift @xs, cname h $cons;
  }
  push @natives,     $_;
  push @native_defs, "$_: " . ('ev(' x @xs) .
                              'mc(' . join(' ', @xs) . ')'
                            . (')' x @xs);
}

my $mc_defs   = $heap[val h $specials{mc}];
my $fn_labels = join ',', map "&&$_", @natives;
my $fns       = join "\n", map "$_;", @native_defs;

my $c = $heap[val h $specials{c}] =~ s/INCLUDES//r
                                  =~ s/MC_DEFS/$mc_defs/r
                                  =~ s/FN_LABELS/$fn_labels/r
                                  =~ s/FNS/$fns/r;

system 'rm -r gen' if -d 'gen';
mkdir 'gen';

open my $fh, '> gen/canard.c';
print $fh $c;
close $fh;

my $sh = $heap[val h $specials{sh}] =~ s/C_SOURCE/$c/r, "\n";

open my $fh, '> gen/canard';
print $fh $sh;
close $fh;

chmod 0755, 'gen/canard';
