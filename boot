#!/usr/bin/env perl
# A bootstrap quasi-interpreter that suffices to coerce the self-code into a
# working C program. Basically an external JIT host for the first (and lame)
# compilation cycle.

open my $fh, '< src/self.canard.sdoc';
my $self = join "\n", grep /^\s*[^\s|A-Z]/, split /\n\n/, join '', <$fh>;
close $fh;

my @parsed = 0;
my @heap;
while ($self =~ /\G\s*(""([^"]+)"<<(.+)[\s\S]*?\n\3|[^]["\s][^][\s]*|\[|\])/g) {
  # TODO
}
